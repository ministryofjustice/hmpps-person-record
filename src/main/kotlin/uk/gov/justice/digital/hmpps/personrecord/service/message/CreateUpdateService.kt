package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.persistence.OptimisticLockException
import kotlinx.coroutines.runBlocking
import org.springframework.dao.CannotAcquireLockException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.shouldCreateOrUpdate
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.EventLoggingService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class CreateUpdateService(
  private val personService: PersonService,
  private val reclusterService: ReclusterService,
  private val eventLoggingService: EventLoggingService,
) {

  @Retryable(
    backoff = Backoff(random = true, delay = 1000, maxDelay = 2000, multiplier = 1.5),
    retryFor = [
      OptimisticLockException::class,
      CannotAcquireLockException::class, // Needed for tests to pass
    ],
  )
  @Transactional(isolation = Isolation.REPEATABLE_READ)
  fun processPerson(person: Person, event: String?, callback: () -> PersonEntity?) {
    runBlocking {
      val existingPersonEntitySearch: PersonEntity? = callback()
      existingPersonEntitySearch.shouldCreateOrUpdate(
        shouldCreate = {
          handlePersonCreation(person, event)
        },
        shouldUpdate = {
          handlePersonUpdate(person, it, event)
        },
      )
    }
  }

  private fun handlePersonCreation(person: Person, event: String?): PersonEntity {
    val personEntity: PersonEntity = personService.createPersonEntity(person)
    personService.linkRecordToPersonKey(personEntity)

    val processedDataDTO = Person.from(personEntity)

    eventLoggingService.recordEventLog(
      beforePerson = null,
      processedPerson = processedDataDTO,
      uuid = personEntity.personKey?.personId?.toString(),
      eventType = event,
    )

    return personEntity
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity, event: String?): PersonEntity {
    val beforeDataDTO = Person.from(existingPersonEntity)
    val updatedPerson = personService.updatePersonEntity(person, existingPersonEntity)
    val processedDataDTO = Person.from(updatedPerson)

    eventLoggingService.recordEventLog(
      beforePerson = beforeDataDTO,
      processedPerson = processedDataDTO,
      uuid = existingPersonEntity.personKey?.personId?.toString(),
      eventType = event,
    )

    updatedPerson.personKey?.let { reclusterService.recluster(it, changedRecord = updatedPerson) }
    return updatedPerson
  }
}
