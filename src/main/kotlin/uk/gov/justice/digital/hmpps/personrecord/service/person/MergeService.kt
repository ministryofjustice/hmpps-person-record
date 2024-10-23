package uk.gov.justice.digital.hmpps.personrecord.service.person

import jakarta.transaction.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.MergeEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MERGE_RECORD_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED

@Service
class MergeService(
    private val telemetryService: TelemetryService,
    private val personRepository: PersonRepository,
    @Value("\${retry.delay}") private val retryDelay: Long,
) {

  @Transactional
  fun processMerge(mergeEvent: MergeEvent, sourcePersonCallback: () -> PersonEntity?, targetPersonCallback: () -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      processMergingOfRecords(mergeEvent, sourcePersonCallback, targetPersonCallback)
    }
  }

  private suspend fun processMergingOfRecords(mergeEvent: MergeEvent, sourcePersonCallback: () -> PersonEntity?, targetPersonCallback: () -> PersonEntity?) {
    val (sourcePersonEntity, targetPersonEntity) = collectPeople(sourcePersonCallback, targetPersonCallback)
    when {
      targetPersonEntity == null -> handleTargetRecordNotFound(mergeEvent)
      sourcePersonEntity == null -> handleSourceRecordNotFound(mergeEvent, targetPersonEntity)
      isSameUuid(sourcePersonEntity, targetPersonEntity) -> handleMergeWithSameUuids(mergeEvent, sourcePersonEntity, targetPersonEntity)
      else -> handleMergeWithDifferentUuids(mergeEvent, sourcePersonEntity, targetPersonEntity)
    }
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
      removeUuidLinkFromRecord(sourcePerson!!)
      updateAndLinkRecords(mergeEvent, sourcePerson, targetPerson)
    }
  }

  private fun handleTargetRecordNotFound(mergeEvent: MergeEvent) {
    telemetryService.trackEvent(
      CPR_MERGE_RECORD_NOT_FOUND,
      mapOf(
        EventKeys.RECORD_TYPE to RecordType.TARGET.name,
        mergeEvent.sourceSystemId.first to mergeEvent.sourceSystemId.second,
        mergeEvent.targetSystemId.first to mergeEvent.targetSystemId.second,
        EventKeys.SOURCE_SYSTEM to mergeEvent.mergedRecord.sourceSystemType.name,
      ),
    )
  }

  private fun handleSourceRecordNotFound(mergeEvent: MergeEvent, targetPersonEntity: PersonEntity) {
    telemetryService.trackEvent(
      CPR_MERGE_RECORD_NOT_FOUND,
      mapOf(
        EventKeys.RECORD_TYPE to RecordType.SOURCE.name,
        mergeEvent.sourceSystemId.first to mergeEvent.sourceSystemId.second,
        mergeEvent.targetSystemId.first to mergeEvent.targetSystemId.second,
        EventKeys.SOURCE_SYSTEM to mergeEvent.mergedRecord.sourceSystemType.name,
      ),
    )
    mergeRecord(mergeEvent, null, targetPersonEntity) { _, targetPerson ->
      updateTargetRecord(mergeEvent, targetPerson)
    }
  }

  private fun mergeRecord(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity, mergeAction: (sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity) -> Unit) {
    val initialSourceUuid = sourcePersonEntity?.personKey?.let { it.personId.toString() }
    val initialTargetUuid = targetPersonEntity.personKey?.let { it.personId.toString() }
    mergeAction(sourcePersonEntity, targetPersonEntity)
    telemetryService.trackEvent(
      CPR_RECORD_MERGED,
      mapOf(
        EventKeys.TO_UUID to initialTargetUuid,
        EventKeys.FROM_UUID to initialSourceUuid,
        mergeEvent.sourceSystemId.first to mergeEvent.sourceSystemId.second,
        mergeEvent.targetSystemId.first to mergeEvent.targetSystemId.second,
        EventKeys.SOURCE_SYSTEM to mergeEvent.mergedRecord.sourceSystemType.name,
      ),
    )
  }

  private fun removeUuidLinkFromRecord(entity: PersonEntity) {
    entity.personKey?.personEntities?.remove(entity)
    entity.personKey = null
    personRepository.saveAndFlush(entity)
  }

  private fun updateTargetRecord(mergeEvent: MergeEvent, targetPersonEntity: PersonEntity) {
    targetPersonEntity.update(mergeEvent.mergedRecord)
    personRepository.save(targetPersonEntity)
  }

  private fun updateAndLinkRecords(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    sourcePersonEntity.mergedTo = targetPersonEntity.id
    targetPersonEntity.update(mergeEvent.mergedRecord)
    personRepository.saveAll(listOf(targetPersonEntity, sourcePersonEntity))
  }

  private fun linkSourceUuidToTargetAndMarkAsMerged(sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    sourcePersonEntity.personKey?.mergedTo = targetPersonEntity.personKey?.id
    sourcePersonEntity.personKey?.status = UUIDStatusType.MERGED
    personRepository.save(sourcePersonEntity)
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

    const val MAX_ATTEMPTS: Int = 5
  }
}
