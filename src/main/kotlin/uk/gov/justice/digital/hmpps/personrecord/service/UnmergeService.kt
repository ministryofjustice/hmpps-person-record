package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.transaction.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideMarkerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.service.MergeService.Companion.MAX_ATTEMPTS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry

@Service
class UnmergeService(
  private val personService: PersonService,
  private val personRepository: PersonRepository,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  @Transactional
  fun processUnmerge(event: String, reactivatedPerson: Person, unmergedPerson: Person) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      processUnmergingOfRecords(event, reactivatedPerson, unmergedPerson)
    }
  }

  private suspend fun processUnmergingOfRecords(event: String, reactivatedPerson: Person, unmergedPerson: Person) {
    val (reactivatedPersonEntity, unmergedPersonEntity) = collectPersonEntities(event, reactivatedPerson, unmergedPerson)
    addExcludeOverrideMarkers(reactivatedPersonEntity, unmergedPersonEntity)
  }

  private fun addExcludeOverrideMarkers(reactivatedPerson: PersonEntity, unmergedPerson: PersonEntity) {
    reactivatedPerson.overrideMarkers.add(
      OverrideMarkerEntity(markerType = OverrideMarkerType.EXCLUDE, markerValue = unmergedPerson.id),
    )
    unmergedPerson.overrideMarkers.add(
      OverrideMarkerEntity(markerType = OverrideMarkerType.EXCLUDE, markerValue = reactivatedPerson.id),
    )
  }

  private suspend fun collectPersonEntities(event: String, reactivatedPerson: Person, unmergedPerson: Person): Pair<PersonEntity, PersonEntity> {
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
}
