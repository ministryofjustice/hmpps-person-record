package uk.gov.justice.digital.hmpps.personrecord.service.person

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_DELETED

@Component
class DeletionService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  fun processDelete(personCallback: () -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      personCallback()?.let {
        handleDeletion(it)
      }
    }
  }

  private fun handleDeletion(personEntity: PersonEntity) {
    handlePersonKeyDeletion(personEntity)
    deletePersonRecord(personEntity)
    handleMergedRecords(personEntity)
  }

  private fun handleMergedRecords(personEntity: PersonEntity) {
    personEntity.id?.let {
      val mergedRecords: List<PersonEntity?> = personRepository.findByMergedTo(it)
      mergedRecords.forEach {
        processDelete { it }
      }
    }
  }

  private fun deletePersonRecord(personEntity: PersonEntity) {
    personRepository.delete(personEntity)
    telemetryService.trackPersonEvent(
      CPR_RECORD_DELETED,
      personEntity,
      mapOf(EventKeys.UUID to personEntity.personKey?.let { it.personId.toString() }),
    )
  }

  private fun handlePersonKeyDeletion(personEntity: PersonEntity) {
    personEntity.personKey?.let {
      when {
        it.personEntities.size == 1 -> deletePersonKey(it, personEntity)
        else -> removeLinkToRecord(personEntity)
      }
    }
  }

  private fun deletePersonKey(personKeyEntity: PersonKeyEntity, personEntity: PersonEntity) {
    personKeyRepository.delete(personKeyEntity)
    telemetryService.trackPersonEvent(
      CPR_UUID_DELETED,
      personEntity,
      mapOf(EventKeys.UUID to personEntity.personKey?.let { it.personId.toString() }),
    )
  }

  private fun removeLinkToRecord(personEntity: PersonEntity) {
    personEntity.personKey?.personEntities?.remove(personEntity)
    personKeyRepository.saveAndFlush(personEntity.personKey!!)
  }

  companion object {
    const val MAX_ATTEMPTS: Int = 5
  }
}
