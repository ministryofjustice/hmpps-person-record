package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.transaction.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.MergeService.Companion.MAX_ATTEMPTS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class UnmergeService(
  private val telemetryService: TelemetryService,
  private val personService: PersonService,
  private val personRepository: PersonRepository,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  @Transactional
  fun processUnmerge(reactivatedPerson: Person, unmergedPerson: Person) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      processUnmergingOfRecords(reactivatedPerson, unmergedPerson)
    }
  }

  private suspend fun processUnmergingOfRecords(reactivatedPerson: Person, unmergedPerson: Person) {
    val (reactivatedPersonEntity, unmergedPersonEntity) = collectPersonEntities(reactivatedPerson, unmergedPerson)
  }

  private suspend fun collectPersonEntities(reactivatedPerson: Person, unmergedPerson: Person): Pair<PersonEntity?, PersonEntity?> {
    return coroutineScope {
      val deferredReactivatedPersonSearch = async {
        personService.processMessage(reactivatedPerson) {
          personRepository.findByCrn(reactivatedPerson.crn!!)
        }
      }
      val deferredUnmergedPersonSearch = async {
        personService.processMessage(unmergedPerson) {
          personRepository.findByCrn(unmergedPerson.crn!!)
        }
      }
      Pair(deferredReactivatedPersonSearch.await(), deferredUnmergedPersonSearch.await())
    }
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    person: Person,
    elementMap: Map<EventKeys, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      EventKeys.SOURCE_SYSTEM to person.sourceSystemType.name,
      EventKeys.DEFENDANT_ID to person.defendantId,
      EventKeys.CRN to person.crn,
      EventKeys.PRISON_NUMBER to person.prisonNumber,
    )
    telemetryService.trackEvent(eventType, identifierMap + elementMap)
  }
}
