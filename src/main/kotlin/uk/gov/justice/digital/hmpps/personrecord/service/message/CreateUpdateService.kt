package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.persistence.OptimisticLockException
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.shouldCreateOrUpdate
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class CreateUpdateService(
  private val personService: PersonService,
  private val reclusterService: ReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  @Retryable(
    backoff = Backoff(random = true, delay = 1000, maxDelay = 2000, multiplier = 1.5),
    retryFor = [
      OptimisticLockException::class,
      DataIntegrityViolationException::class,
      CannotAcquireLockException::class, // Needed for tests to pass
    ],
  )
  @Transactional(isolation = REPEATABLE_READ)
  fun processPerson(person: Person, findPerson: () -> PersonEntity?): PersonEntity = runBlocking {
    return@runBlocking findPerson().shouldCreateOrUpdate(
      shouldCreate = {
        handlePersonCreation(person)
      },
      shouldUpdate = {
        handlePersonUpdate(person, it)
      },
    )
  }

  private fun handlePersonCreation(person: Person): PersonEntity {
    val personEntity: PersonEntity = personService.createPersonEntity(person)
    val linkedPersonEntity = personService.linkRecordToPersonKey(personEntity)
    publisher.publishEvent(PersonCreated(linkedPersonEntity))
    return linkedPersonEntity
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity): PersonEntity {
    val updatedPersonEntity = personService.updatePersonEntity(person, existingPersonEntity)
    publisher.publishEvent(PersonUpdated(updatedPersonEntity))
    updatedPersonEntity.personKey?.let { reclusterService.recluster(it, changedRecord = updatedPersonEntity) }
    return updatedPersonEntity
  }
}
