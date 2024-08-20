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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.PersonService.Companion.MAX_ATTEMPTS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
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

  fun processMerge(mergedRecord: Person, sourcePersonCallback: () -> PersonEntity?, targetPersonCallback: () -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, retryExceptions) {
      runBlocking {
        processMergingOfRecords(mergedRecord, sourcePersonCallback, targetPersonCallback)
      }
    }
  }

  private suspend fun processMergingOfRecords(mergedRecord: Person, sourcePersonCallback: () -> PersonEntity?, targetPersonCallback: () -> PersonEntity?) {
    val (sourcePersonEntity, targetPersonEntity) = collectPeople(sourcePersonCallback, targetPersonCallback)
    when {
      isSameUuid(sourcePersonEntity, targetPersonEntity) -> mergeRecord(mergedRecord, sourcePersonEntity, targetPersonEntity)
    }
  }

  private fun mergeRecord(mergedRecord: Person, sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity?) {
    updateAndLinkRecords(mergedRecord, sourcePersonEntity, targetPersonEntity)
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

  private fun updateAndLinkRecords(mergedRecord: Person, sourcePersonEntity: PersonEntity?, targetPersonEntity: PersonEntity?) {
    targetPersonEntity?.update(mergedRecord)
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
}
