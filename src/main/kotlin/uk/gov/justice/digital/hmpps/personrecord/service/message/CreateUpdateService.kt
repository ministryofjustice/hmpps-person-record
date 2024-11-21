package uk.gov.justice.digital.hmpps.personrecord.service.message

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
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
    return personEntity
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity, event: String?): PersonEntity {
    if (isCreateEvent(event)) {
      telemetryService.trackPersonEvent(CPR_NEW_RECORD_EXISTS, person)
    }
    return personService.updatePersonEntity(person, existingPersonEntity)
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
