package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.MergeEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.PersonService.Companion.MAX_ATTEMPTS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MERGE_RECORD_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED

@Service
class MergeService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  private val retryExceptions = listOf(
    ObjectOptimisticLockingFailureException::class,
    CannotAcquireLockException::class,
    JpaSystemException::class,
    JpaObjectRetrievalFailureException::class,
    DataIntegrityViolationException::class,
    ConstraintViolationException::class,
  )

  fun processMerge(mergeEvent: MergeEvent, sourcePersonCallback: () -> PersonEntity?, targetPersonCallback: () -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, retryExceptions) {
      processMergingOfRecords(mergeEvent, sourcePersonCallback, targetPersonCallback)
    }
  }

  private suspend fun processMergingOfRecords(mergeEvent: MergeEvent, sourcePersonCallback: () -> PersonEntity?, targetPersonCallback: () -> PersonEntity?) {
    val (sourcePersonEntity, targetPersonEntity) = collectPeople(sourcePersonCallback, targetPersonCallback)
    when {
      targetPersonEntity == null -> handleTargetRecordNotFound(mergeEvent)
      else -> when {
        isSameOrNullSourceUuid(sourcePersonEntity, targetPersonEntity) -> mergeRecord(mergeEvent, sourcePersonEntity, targetPersonEntity)
        else -> handleMergeWithDifferentUuids(mergeEvent, sourcePersonEntity!!, targetPersonEntity)
      }
    }
  }

  private fun handleMergeWithDifferentUuids(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    when {
      sourcePersonKeyHasMultipleRecords(sourcePersonEntity) -> return
      else -> handleSourceUuidWithSingleRecord(mergeEvent, sourcePersonEntity, targetPersonEntity)
    }
  }

  private fun handleSourceUuidWithSingleRecord(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    removeLinkFromRecord(sourcePersonEntity)
    mergeRecord(mergeEvent, sourcePersonEntity, targetPersonEntity)
  }

  private fun removeLinkFromRecord(entity: PersonEntity) {
    entity.personKey = null
    personRepository.saveAndFlush(entity)
  }

  private fun handleTargetRecordNotFound(mergeEvent: MergeEvent) {
    telemetryService.trackEvent(
      CPR_MERGE_RECORD_NOT_FOUND,
      mapOf(
        EventKeys.RECORD_TYPE to RecordType.TARGET.name,
        mergeEvent.sourceSystemId.first to mergeEvent.sourceSystemId.second,
        mergeEvent.targetSystemId.first to mergeEvent.targetSystemId.second,
        EventKeys.FROM_SOURCE_SYSTEM to mergeEvent.mergedRecord.sourceSystemType.name,
        EventKeys.TO_SOURCE_SYSTEM to mergeEvent.mergedRecord.sourceSystemType.name,
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
        EventKeys.FROM_SOURCE_SYSTEM to mergeEvent.mergedRecord.sourceSystemType.name,
        EventKeys.TO_SOURCE_SYSTEM to mergeEvent.mergedRecord.sourceSystemType.name,
      ),
    )
    updateTargetRecord(mergeEvent, targetPersonEntity)
  }

  private fun mergeRecord(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity) {
    when {
      sourcePersonEntity == null -> handleSourceRecordNotFound(mergeEvent, targetPersonEntity)
      else -> updateAndLinkRecords(mergeEvent, sourcePersonEntity, targetPersonEntity)
    }
    telemetryService.trackEvent(
      CPR_RECORD_MERGED,
      mapOf(
        EventKeys.TO_UUID to targetPersonEntity.personKey?.let { it.personId.toString() },
        EventKeys.FROM_UUID to sourcePersonEntity?.personKey?.let { it.personId.toString() },
        EventKeys.TO_SOURCE_SYSTEM to targetPersonEntity.sourceSystem.name,
        EventKeys.FROM_SOURCE_SYSTEM to sourcePersonEntity?.sourceSystem?.name,
      ),
    )
  }

  private fun updateTargetRecord(mergeEvent: MergeEvent, targetPersonEntity: PersonEntity) {
    targetPersonEntity.update(mergeEvent.mergedRecord)
    personRepository.saveAndFlush(targetPersonEntity)
  }

  private fun updateAndLinkRecords(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    sourcePersonEntity.mergedTo = targetPersonEntity.id
    targetPersonEntity.update(mergeEvent.mergedRecord)
    personRepository.saveAllAndFlush(listOf(targetPersonEntity, sourcePersonEntity))
  }

  private fun isSameOrNullSourceUuid(sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity?): Boolean =
    (sourcePersonEntity == PersonEntity.empty && targetPersonEntity != PersonEntity.empty) || (sourcePersonEntity?.personKey?.id == targetPersonEntity?.personKey?.id)

  private fun sourcePersonKeyHasMultipleRecords(sourcePersonEntity: PersonEntity?): Boolean = sourcePersonEntity?.personKey?.personEntities?.size!! > 1

  private suspend fun collectPeople(sourcePersonCallback: () -> PersonEntity?, targetPersonCallback: () -> PersonEntity?): Pair<PersonEntity?, PersonEntity?> {
    return coroutineScope {
      Pair(
        async {
          sourcePersonCallback()
        }.await(),
        async {
          targetPersonCallback()
        }.await(),
      )
    }
  }

  companion object {
    enum class RecordType {
      TARGET,
      SOURCE,
    }
  }
}
