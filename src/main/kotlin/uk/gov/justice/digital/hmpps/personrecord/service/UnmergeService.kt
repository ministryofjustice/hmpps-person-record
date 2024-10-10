package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.transaction.Transactional
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideMarkerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class UnmergeService(
  private val telemetryService: TelemetryService,
  private val personService: PersonService,
  private val personKeyService: PersonKeyService,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  @Transactional
  fun processUnmerge(event: String, reactivatedPerson: Person, unmergedPerson: Person, reactivatedPersonCallback: () -> PersonEntity?, unmergedPersonCallback: () -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      processUnmergingOfRecords(event, reactivatedPerson, unmergedPerson, reactivatedPersonCallback, unmergedPersonCallback)
    }
  }

  private suspend fun processUnmergingOfRecords(event: String, reactivatedPerson: Person, unmergedPerson: Person, reactivatedPersonCallback: () -> PersonEntity?, unmergedPersonCallback: () -> PersonEntity?) {
    val unmergedPersonEntity = retrieveUnmergedPerson(event, unmergedPerson, unmergedPersonCallback)
    val reactivatedPersonEntity = retrieveReactivatedPerson(event, reactivatedPerson, reactivatedPersonCallback)
    unmergeRecords(unmergedPersonEntity, reactivatedPersonEntity)
  }

  private fun retrieveUnmergedPerson(event: String, unmergedPerson: Person, unmergedPersonCallback: () -> PersonEntity?): PersonEntity =
    personService.processMessage(unmergedPerson, event) {
      searchForPersonRecord(unmergedPerson, UnmergeRecordType.UNMERGED, unmergedPersonCallback)
    }

  private fun retrieveReactivatedPerson(event: String, reactivatedPerson: Person, reactivatedPersonCallback: () -> PersonEntity?): PersonEntity =
    personService.processMessage(reactivatedPerson, event, linkRecord = false) {
      searchForPersonRecord(reactivatedPerson, UnmergeRecordType.REACTIVATED, reactivatedPersonCallback)
    }

  private fun unmergeRecords(unmergedPersonEntity: PersonEntity, reactivatedPersonEntity: PersonEntity) {
    if (clusterDoesNotHadAdditionalRecords(unmergedPersonEntity, reactivatedPersonEntity)) {
      personKeyService.setPersonKeyStatus(unmergedPersonEntity.personKey!!, UUIDStatusType.NEEDS_ATTENTION)
    }
    addExcludeOverrideMarkers(reactivatedPersonEntity, unmergedPersonEntity)
  }

  private fun searchForPersonRecord(person: Person, recordType: UnmergeRecordType, callback: () -> PersonEntity?): PersonEntity? {
    val personEntity = callback()
    if (personEntity == null) {
      telemetryService.trackEventWithIds(
        TelemetryEventType.CPR_UNMERGE_RECORD_NOT_FOUND,
        person,
        mapOf(EventKeys.RECORD_TYPE to recordType.name),
      )
    }
    return personEntity
  }

  private fun clusterDoesNotHadAdditionalRecords(unmergedPersonEntity: PersonEntity, reactivatedPersonEntity: PersonEntity): Boolean {
    val additionalRecords = unmergedPersonEntity.personKey?.let {
      it.personEntities.filter {
        listOf(unmergedPersonEntity.id!!, reactivatedPersonEntity.id!!).contains(it.id).not()
      }
    }
    return (additionalRecords?.size ?: 0) > 0
  }

  private fun addExcludeOverrideMarkers(reactivatedPerson: PersonEntity, unmergedPerson: PersonEntity) {
    reactivatedPerson.overrideMarkers.add(
      OverrideMarkerEntity(markerType = OverrideMarkerType.EXCLUDE, markerValue = unmergedPerson.id, person = reactivatedPerson),
    )
    unmergedPerson.overrideMarkers.add(
      OverrideMarkerEntity(markerType = OverrideMarkerType.EXCLUDE, markerValue = reactivatedPerson.id, person = unmergedPerson),
    )
  }

  companion object {
    enum class UnmergeRecordType {
      REACTIVATED,
      UNMERGED,
    }
    const val MAX_ATTEMPTS: Int = 5
  }
}
