package uk.gov.justice.digital.hmpps.personrecord.service.message

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.EventLoggingService
import uk.gov.justice.digital.hmpps.personrecord.service.ReadWriteLockService
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DETAILS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_NEW_RECORD_EXISTS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UPDATE_RECORD_DOES_NOT_EXIST

@Component
class CreateUpdateService(
  private val telemetryService: TelemetryService,
  private val personService: PersonService,
  private val personKeyService: PersonKeyService,
  private val readWriteLockService: ReadWriteLockService,
  @Value("\${retry.delay}") private val retryDelay: Long,
  private val eventLoggingService: EventLoggingService,
  private val objectMapper: ObjectMapper,

) {

  fun processMessage(person: Person, event: String? = null, linkRecord: Boolean = true, callback: () -> PersonEntity?): PersonEntity = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      readWriteLockService.withWriteLock(person.sourceSystemType) {
        return@withWriteLock processPerson(person, event, linkRecord, callback)
      }
    }
  }

  private fun processPerson(person: Person, event: String?, linkRecord: Boolean, callback: () -> PersonEntity?): PersonEntity {
    val existingPersonEntity: PersonEntity? = callback()
    val personEntity: PersonEntity = when {
      (existingPersonEntity == null) -> handlePersonCreation(person, event, linkRecord)
      else -> handlePersonUpdate(person, existingPersonEntity, event)
    }
    return personEntity
  }

  private fun handlePersonCreation(person: Person, event: String?, linkRecord: Boolean): PersonEntity {
    if (isUpdateEvent(event)) {
      telemetryService.trackPersonEvent(CPR_UPDATE_RECORD_DOES_NOT_EXIST, person)
    }
    val personEntity: PersonEntity = personService.createPersonEntity(person)
    val personKey: PersonKeyEntity? = when {
      linkRecord -> personKeyService.getPersonKey(personEntity)
      else -> PersonKeyEntity.empty
    }
    personService.linkPersonEntityToPersonKey(personEntity, personKey)
    val processedDataDTO = Person.from(personEntity)
    val processedData = objectMapper.writeValueAsString(processedDataDTO)

    eventLoggingService.recordEventLog(
      beforeData = null,
      processedData = processedData,
      uuid = personEntity.personKey?.personId?.toString(),
      sourceSystem = personEntity.sourceSystem.toString(),
      messageEventType = event,
      processedPerson = processedDataDTO,
    )

    return personEntity
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity, event: String?): PersonEntity {
    if (isCreateEvent(event)) {
      telemetryService.trackPersonEvent(CPR_NEW_RECORD_EXISTS, person)
    }
    val beforeDataDTO = Person.from(existingPersonEntity)
    val beforeData = objectMapper.writeValueAsString(beforeDataDTO)

    val updatedPerson = personService.updatePersonEntity(person, existingPersonEntity)

    val processedDataDTO = Person.from(updatedPerson)
    val processedData = objectMapper.writeValueAsString(processedDataDTO)

    eventLoggingService.recordEventLog(
      beforeData = beforeData,
      processedData = processedData,
      uuid = existingPersonEntity.personKey?.personId?.toString(),
      sourceSystem = existingPersonEntity.sourceSystem.toString(),
      messageEventType = event,
      processedPerson = processedDataDTO,
    )
    return updatedPerson
  }

  private fun isUpdateEvent(event: String?) = listOf(
    PRISONER_UPDATED,
    OFFENDER_DETAILS_CHANGED,
    OFFENDER_ALIAS_CHANGED,
    OFFENDER_ADDRESS_CHANGED,
  ).contains(event)

  private fun isCreateEvent(event: String?) = listOf(PRISONER_CREATED, NEW_OFFENDER_CREATED).contains(event)

  companion object {
    private const val MAX_ATTEMPTS = 5
  }
}
