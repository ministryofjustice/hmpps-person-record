package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.transaction.Transactional
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideMarkerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.MergeService.Companion.MAX_ATTEMPTS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry

@Service
class UnmergeService(
  private val personService: PersonService,
  private val personKeyService: PersonKeyService,
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
    val unmergedPersonEntity = processUnmergedPerson(event, unmergedPerson)
    val reactivatedPersonEntity = processReactivatedPerson(event, reactivatedPerson)
    addExcludeOverrideMarkers(reactivatedPersonEntity, unmergedPersonEntity)
  }

  private fun processUnmergedPerson(event: String, unmergedPerson: Person): PersonEntity {
    val unmergedPersonEntity = personService.processMessage(unmergedPerson, event) {
      personRepository.findByCrn(unmergedPerson.crn!!)
    }
    if (isNotSingleRecordCluster(unmergedPersonEntity)) {
      personKeyService.setPersonKeyStatus(unmergedPersonEntity.personKey!!, UUIDStatusType.NEEDS_ATTENTION)
    }
    return unmergedPersonEntity
  }

  private fun processReactivatedPerson(event: String, reactivatedPerson: Person): PersonEntity {
    val reactivatedPersonEntity = personService.processMessage(reactivatedPerson, event, linkRecord = false) {
      personRepository.findByCrn(reactivatedPerson.crn!!)
    }
    // CPR-399
    return reactivatedPersonEntity
  }

  private fun isNotSingleRecordCluster(personEntity: PersonEntity): Boolean = (personEntity.personKey?.personEntities?.size ?: 0) > 1

  private fun addExcludeOverrideMarkers(reactivatedPerson: PersonEntity, unmergedPerson: PersonEntity) {
    reactivatedPerson.overrideMarkers.add(
      OverrideMarkerEntity(markerType = OverrideMarkerType.EXCLUDE, markerValue = unmergedPerson.id),
    )
    unmergedPerson.overrideMarkers.add(
      OverrideMarkerEntity(markerType = OverrideMarkerType.EXCLUDE, markerValue = reactivatedPerson.id),
    )
  }
}
