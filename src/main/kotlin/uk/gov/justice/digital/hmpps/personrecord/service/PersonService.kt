package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.runBlocking
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class PersonService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val readWriteLockService: ReadWriteLockService,
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

  fun processMessage(person: Person, callback: () -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, retryExceptions) {
      readWriteLockService.withWriteLock { processPerson(person, callback) }
    }
  }

  private fun processPerson(person: Person, callback: () -> PersonEntity?) {
    val existingPersonEntities: PersonEntity? = callback()
    when {
      (existingPersonEntities == null) -> handlePersonCreation(person)
      else -> handlePersonUpdate(person, existingPersonEntities)
    }
  }

  private fun handlePersonCreation(person: Person) {
    createPersonEntity(person)
    trackEvent(TelemetryEventType.CPR_RECORD_CREATED, person)
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity) {
    updateExistingPersonEntity(person, existingPersonEntity)
    trackEvent(TelemetryEventType.CPR_RECORD_UPDATED, person)
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity) {
    removeAllChildEntities(personEntity)
    val updatedPersonEntity = personEntity.update(person)
    personRepository.saveAndFlush(updatedPersonEntity)
  }

  private fun removeAllChildEntities(personEntity: PersonEntity): PersonEntity {
    personEntity.aliases.clear()
    personEntity.addresses.clear()
    personEntity.contacts.clear()
    return personRepository.saveAndFlush(personEntity)
  }


  private fun createPersonEntity(person: Person) {
    val personEntity = PersonEntity.from(person)
    personRepository.saveAndFlush(personEntity)
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    person: Person,
    elementMap: Map<String, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      "SourceSystem" to person.sourceSystemType.name,
      "DefendantId" to person.defendantId,
      "CRN" to (person.otherIdentifiers?.crn ?: ""),
    )
    telemetryService.trackEvent(eventType, identifierMap + elementMap)
  }

  companion object {
    const val MAX_ATTEMPTS: Int = 5
  }
}
