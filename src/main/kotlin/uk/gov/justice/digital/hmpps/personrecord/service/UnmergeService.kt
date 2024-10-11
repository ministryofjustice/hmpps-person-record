package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.transaction.Transactional
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.UnmergeEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideMarkerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
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
  private val personRepository: PersonRepository,
  private val personKeyService: PersonKeyService,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  @Transactional
  fun processUnmerge(unmergeEvent: UnmergeEvent, reactivatedPersonCallback: () -> PersonEntity?, unmergedPersonCallback: () -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      processUnmergingOfRecords(unmergeEvent, reactivatedPersonCallback, unmergedPersonCallback)
    }
  }

  private suspend fun processUnmergingOfRecords(unmergeEvent: UnmergeEvent, reactivatedPersonCallback: () -> PersonEntity?, unmergedPersonCallback: () -> PersonEntity?) {
    val unmergedPersonEntity = retrieveUnmergedPerson(unmergeEvent, unmergedPersonCallback)
    val reactivatedPersonEntity = retrieveReactivatedPerson(unmergeEvent, reactivatedPersonCallback)
    unmergeRecords(unmergeEvent, unmergedPersonEntity, reactivatedPersonEntity)
  }

  private fun retrieveUnmergedPerson(unmergeEvent: UnmergeEvent, unmergedPersonCallback: () -> PersonEntity?): PersonEntity =
    personService.processMessage(unmergeEvent.unmergedRecord, unmergeEvent.event) {
      searchForPersonRecord(
        unmergeEvent.unmergedRecord,
        unmergeEvent.unmergedSystemId,
        UnmergeRecordType.UNMERGED,
        unmergedPersonCallback,
      )
    }

  private fun retrieveReactivatedPerson(unmergeEvent: UnmergeEvent, reactivatedPersonCallback: () -> PersonEntity?): PersonEntity =
    personService.processMessage(unmergeEvent.reactivatedRecord, unmergeEvent.event, linkRecord = false) {
      searchForPersonRecord(
        unmergeEvent.reactivatedRecord,
        unmergeEvent.reactivatedSystemId,
        UnmergeRecordType.REACTIVATED,
        reactivatedPersonCallback,
      )
    }

  private fun unmergeRecords(unmergeEvent: UnmergeEvent, unmergedPersonEntity: PersonEntity, reactivatedPersonEntity: PersonEntity) {
    if (clusterDoesNotHadAdditionalRecords(unmergedPersonEntity, reactivatedPersonEntity)) {
      personKeyService.setPersonKeyStatus(unmergedPersonEntity.personKey!!, UUIDStatusType.NEEDS_ATTENTION)
    }
    when {
      mergeLinkExists(reactivatedPersonEntity) -> removeMergedToLink(reactivatedPersonEntity)
      else -> telemetryService.trackEvent(
        TelemetryEventType.CPR_UNMERGE_LINK_NOT_FOUND,
        mapOf(
          unmergeEvent.unmergedSystemId.first to unmergeEvent.unmergedSystemId.second,
          unmergeEvent.reactivatedSystemId.first to unmergeEvent.reactivatedSystemId.second,
          EventKeys.SOURCE_SYSTEM to reactivatedPersonEntity.sourceSystem.name,
          EventKeys.RECORD_TYPE to UnmergeRecordType.REACTIVATED.name,
        ),
      )
    }
    addExcludeOverrideMarkers(reactivatedPersonEntity, unmergedPersonEntity)
    findAndAssignUuid(reactivatedPersonEntity)
  }

  private fun searchForPersonRecord(record: Person, systemId: Pair<EventKeys, String>, recordType: UnmergeRecordType, callback: () -> PersonEntity?): PersonEntity? {
    val personEntity = callback()
    if (personEntity == null) {
      telemetryService.trackEvent(
        TelemetryEventType.CPR_UNMERGE_RECORD_NOT_FOUND,
        mapOf(
          systemId.first to systemId.second,
          EventKeys.SOURCE_SYSTEM to record.sourceSystemType.name,
          EventKeys.RECORD_TYPE to recordType.name,
        ),
      )
    }
    return personEntity
  }

  private fun findAndAssignUuid(reactivatedPersonEntity: PersonEntity) {
    reactivatedPersonEntity.personKey = personKeyService.getPersonKey(reactivatedPersonEntity)
    personRepository.saveAndFlush(reactivatedPersonEntity)
  }

  private fun removeMergedToLink(personEntity: PersonEntity) {
    if (personEntity.personKey != PersonKeyEntity.empty) {
      personEntity.personKey?.personEntities?.remove(personEntity)
      personEntity.personKey = null
    }
    personEntity.mergedTo = null
    personRepository.saveAndFlush(personEntity)
  }

  private fun clusterDoesNotHadAdditionalRecords(unmergedPersonEntity: PersonEntity, reactivatedPersonEntity: PersonEntity): Boolean {
    val additionalRecords = unmergedPersonEntity.personKey?.let {
      it.personEntities.filter {
        listOf(unmergedPersonEntity.id!!, reactivatedPersonEntity.id!!).contains(it.id).not()
      }
    }
    return (additionalRecords?.size ?: 0) > 0
  }

  private fun mergeLinkExists(personEntity: PersonEntity): Boolean = personEntity.mergedTo != null

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
