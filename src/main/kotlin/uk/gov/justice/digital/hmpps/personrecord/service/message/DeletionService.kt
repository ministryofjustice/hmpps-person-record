package uk.gov.justice.digital.hmpps.personrecord.service.message

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.EventLoggingService
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_DELETED

@Component
class DeletionService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val eventLoggingService: EventLoggingService,
  private val retryExecutor: RetryExecutor,
  private val personMatchClient: PersonMatchClient,
) {

  fun processDelete(event: String?, personCallback: () -> PersonEntity?) = runBlocking {
    retryExecutor.runWithRetryDatabase {
      personCallback()?.let {
        handleDeletion(event, it)
      }
    }
  }

  fun deletePersonFromPersonMatch(personEntity: PersonEntity) = runCatching {
    runBlocking {
      retryExecutor.runWithRetryHTTP { personMatchClient.deletePerson(PersonMatchRecord.from(personEntity)) }
    }
  }

  private fun handleDeletion(event: String?, personEntity: PersonEntity) {
    val beforeDataDTO = Person.from(personEntity)

    handlePersonKeyDeletion(personEntity)
    deletePersonRecord(personEntity)
    deletePersonFromPersonMatch(personEntity)
    handleMergedRecords(event, personEntity)

    eventLoggingService.recordEventLog(
      beforePerson = beforeDataDTO,
      processedPerson = null,
      uuid = personEntity.personKey?.personId.toString(),
      eventType = event,
    )
  }

  private fun handleMergedRecords(event: String?, personEntity: PersonEntity) {
    personEntity.id?.let {
      val mergedRecords: List<PersonEntity?> = personRepository.findByMergedTo(it)
      mergedRecords.forEach { mergedRecord ->
        processDelete(event) { mergedRecord }
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
    personKeyRepository.save(personEntity.personKey!!)
  }
}
