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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.exists
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class CreateUpdateService(
  private val personService: PersonService,
  private val publisher: ApplicationEventPublisher,
) {

  @Retryable(
    backoff = Backoff(delay = 600),
    retryFor = [
      OptimisticLockException::class,
      DataIntegrityViolationException::class,
      CannotAcquireLockException::class,
    ],
  )
  @Transactional(isolation = REPEATABLE_READ)
  fun processPerson(
    person: Person,
    shouldReclusterOnUpdate: Boolean = true,
    shouldLinkOnCreate: Boolean = true,
    findPerson: () -> PersonEntity?,
  ): PersonEntity = runBlocking {
    return@runBlocking findPerson().exists(
      no = {
        handlePersonCreation(person, shouldLinkOnCreate)
      },
      yes = {
        handlePersonUpdate(person, it, shouldReclusterOnUpdate)
      },
    )
  }

  private fun handlePersonCreation(person: Person, shouldLinkOnCreate: Boolean): PersonEntity {
    val personEntity: PersonEntity = personService.createPersonEntity(person)
    if (shouldLinkOnCreate) {
      personService.linkRecordToPersonKey(personEntity)
    }
    publisher.publishEvent(PersonCreated(personEntity))
    return personEntity
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity, shouldReclusterOnUpdate: Boolean): PersonEntity {
    val personUpdated = personService.updatePersonEntity(person, existingPersonEntity, shouldReclusterOnUpdate)
    publisher.publishEvent(personUpdated)
    return personUpdated.personEntity
  }
}
