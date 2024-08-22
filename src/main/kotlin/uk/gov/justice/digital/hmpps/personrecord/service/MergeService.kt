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
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
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
      sourcePersonEntity == null -> return // CPR-341
      targetPersonEntity == null -> handleTargetRecordNotFound(mergeEvent)
      else -> when {
        isSameUuid(sourcePersonEntity, targetPersonEntity) -> mergeRecord(mergeEvent, sourcePersonEntity, targetPersonEntity)
        else -> return // CPR-342
      }
    }
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

  private fun mergeRecord(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity?) {
    updateAndLinkRecords(mergeEvent, sourcePersonEntity, targetPersonEntity)
    telemetryService.trackEvent(
      CPR_RECORD_MERGED,
      mapOf(
        EventKeys.TO_UUID to targetPersonEntity?.personKey?.personId.toString(),
        EventKeys.FROM_UUID to sourcePersonEntity?.personKey?.personId.toString(),
        EventKeys.TO_SOURCE_SYSTEM to targetPersonEntity?.sourceSystem?.name,
        EventKeys.FROM_SOURCE_SYSTEM to sourcePersonEntity?.sourceSystem?.name,
      ),
    )
  }

  private fun updateAndLinkRecords(mergeEvent: MergeEvent, sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity?) {
    targetPersonEntity?.update(mergeEvent.mergedRecord)
    sourcePersonEntity?.mergedTo = targetPersonEntity?.id
    personRepository.saveAllAndFlush(listOf(targetPersonEntity, sourcePersonEntity))
  }

  private fun isSameUuid(sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity?): Boolean = sourcePersonEntity?.personKey?.id == targetPersonEntity?.personKey?.id

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
