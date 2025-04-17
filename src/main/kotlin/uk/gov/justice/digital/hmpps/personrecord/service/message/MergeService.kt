package uk.gov.justice.digital.hmpps.personrecord.service.message

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.MergeEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MERGE_RECORD_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED

@Component
class MergeService(
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val deletionService: DeletionService,
  private val publisher: ApplicationEventPublisher,
) {

  fun processMerge(mergeEvent: MergeEvent, sourcePersonCallback: () -> PersonEntity?, targetPersonCallback: () -> PersonEntity?) = runBlocking {
    processMergingOfRecords(mergeEvent, sourcePersonCallback, targetPersonCallback)
  }

  private suspend fun processMergingOfRecords(mergeEvent: MergeEvent, sourcePersonCallback: () -> PersonEntity?, targetPersonCallback: () -> PersonEntity?): PersonEntity? {
    val (sourcePersonEntity, targetPersonEntity) = collectPeople(sourcePersonCallback, targetPersonCallback)

    when {
      targetPersonEntity == PersonEntity.empty -> handleTargetRecordNotFound(mergeEvent)
      sourcePersonEntity == PersonEntity.empty -> handleSourceRecordNotFound(mergeEvent, targetPersonEntity)
      isSameUuid(sourcePersonEntity, targetPersonEntity) -> handleMergeWithSameUuids(mergeEvent, sourcePersonEntity, targetPersonEntity)
      else -> handleMergeWithDifferentUuids(mergeEvent, sourcePersonEntity, targetPersonEntity)
    }

    sourcePersonEntity?.let { deletionService.deletePersonFromPersonMatch(it) }
    return targetPersonEntity
  }

  private fun handleMergeWithSameUuids(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    mergeRecord(mergeEvent, sourcePersonEntity, targetPersonEntity) { sourcePerson, targetPerson ->
      updateAndLinkRecords(mergeEvent, sourcePerson!!, targetPerson)
    }
  }

  private fun handleMergeWithDifferentUuids(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    when {
      sourcePersonKeyHasMultipleRecords(sourcePersonEntity) -> handleSourceUuidWithMultipleRecords(mergeEvent, sourcePersonEntity, targetPersonEntity)
      else -> handleSourceUuidWithSingleRecord(mergeEvent, sourcePersonEntity, targetPersonEntity)
    }
  }

  private fun handleSourceUuidWithSingleRecord(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    mergeRecord(mergeEvent, sourcePersonEntity, targetPersonEntity) { sourcePerson, targetPerson ->
      linkSourceUuidToTargetAndMarkAsMerged(sourcePerson!!, targetPerson)
      updateAndLinkRecords(mergeEvent, sourcePerson, targetPerson)
    }
  }

  private fun handleSourceUuidWithMultipleRecords(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    mergeRecord(mergeEvent, sourcePersonEntity, targetPersonEntity) { sourcePerson, targetPerson ->
      sourcePerson!!.removePersonKeyLink()
      updateAndLinkRecords(mergeEvent, sourcePerson, targetPerson)
    }
  }

  private fun handleTargetRecordNotFound(mergeEvent: MergeEvent) {
    publisher.publishEvent(
      RecordTelemetry(
        CPR_MERGE_RECORD_NOT_FOUND,
        mapOf(
          EventKeys.RECORD_TYPE to RecordType.TARGET.name,
          mergeEvent.sourceSystemId.first to mergeEvent.sourceSystemId.second,
          mergeEvent.targetSystemId.first to mergeEvent.targetSystemId.second,
          EventKeys.SOURCE_SYSTEM to mergeEvent.mergedRecord.sourceSystem.name,
        ),
      ),
    )
  }

  private fun handleSourceRecordNotFound(mergeEvent: MergeEvent, targetPersonEntity: PersonEntity) {
    publisher.publishEvent(
      RecordTelemetry(
        CPR_MERGE_RECORD_NOT_FOUND,
        mapOf(
          EventKeys.RECORD_TYPE to RecordType.SOURCE.name,
          mergeEvent.sourceSystemId.first to mergeEvent.sourceSystemId.second,
          mergeEvent.targetSystemId.first to mergeEvent.targetSystemId.second,
          EventKeys.SOURCE_SYSTEM to mergeEvent.mergedRecord.sourceSystem.name,
        ),
      ),
    )
    mergeRecord(mergeEvent, PersonEntity.empty, targetPersonEntity) { _, targetPerson ->
      targetPerson.update(mergeEvent.mergedRecord)
    }
  }

  private fun mergeRecord(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity, mergeAction: (sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity) -> Unit) {
    val initialSourceUuid = sourcePersonEntity?.personKey?.let { it.personUUID.toString() }
    val initialTargetUuid = targetPersonEntity.personKey?.let { it.personUUID.toString() }

    mergeAction(sourcePersonEntity, targetPersonEntity)

    sourcePersonEntity?.let {
      publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_MERGED, it))
      personRepository.save(it)
    }
    personRepository.save(targetPersonEntity)

    publisher.publishEvent(
      RecordTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          EventKeys.TO_UUID to initialTargetUuid,
          EventKeys.FROM_UUID to initialSourceUuid,
          mergeEvent.sourceSystemId.first to mergeEvent.sourceSystemId.second,
          mergeEvent.targetSystemId.first to mergeEvent.targetSystemId.second,
          EventKeys.SOURCE_SYSTEM to mergeEvent.mergedRecord.sourceSystem.name,
        ),
      ),
    )
  }

  private fun updateAndLinkRecords(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    sourcePersonEntity.mergedTo = targetPersonEntity.id
    targetPersonEntity.update(mergeEvent.mergedRecord)
  }

  private fun linkSourceUuidToTargetAndMarkAsMerged(sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    sourcePersonEntity.personKey?.let {
      it.apply {
        mergedTo = targetPersonEntity.personKey?.id
        status = UUIDStatusType.MERGED
      }
      personKeyRepository.save(it)
    }
  }

  private fun isSameUuid(sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity?): Boolean = sourcePersonEntity?.personKey?.id == targetPersonEntity?.personKey?.id

  private fun sourcePersonKeyHasMultipleRecords(sourcePersonEntity: PersonEntity?): Boolean = (sourcePersonEntity?.personKey?.personEntities?.size ?: 0) > 1

  private suspend fun collectPeople(sourcePersonCallback: () -> PersonEntity?, targetPersonCallback: () -> PersonEntity?): Pair<PersonEntity?, PersonEntity?> = coroutineScope {
    val deferredSourcePersonCallback = async { sourcePersonCallback() }
    val deferredTargetSourceCallback = async { targetPersonCallback() }
    return@coroutineScope Pair(deferredSourcePersonCallback.await(), deferredTargetSourceCallback.await())
  }

  companion object {
    enum class RecordType {
      TARGET,
      SOURCE,
    }
  }
}
