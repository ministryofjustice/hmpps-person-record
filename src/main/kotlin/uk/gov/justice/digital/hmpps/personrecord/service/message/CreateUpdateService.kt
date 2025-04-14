package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.persistence.OptimisticLockException
import kotlinx.coroutines.runBlocking
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
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class CreateUpdateService(
  private val personService: PersonService,
  private val reclusterService: ReclusterService,
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
  fun processPerson(person: Person, findPerson: () -> PersonEntity?) {
    runBlocking {
      val existingPersonEntitySearch: PersonEntity? = findPerson()
      val personEntity: PersonEntity = existingPersonEntitySearch.shouldCreateOrUpdate(
        shouldCreate = {
          handlePersonCreation(person)
        },
        shouldUpdate = {
          handlePersonUpdate(person, it)
        },
      )
      return@runBlocking personEntity
    }
  }

  private fun handlePersonCreation(person: Person): PersonEntity {
    val personEntity: PersonEntity = personService.createPersonEntity(person)
    personService.linkRecordToPersonKey(personEntity)
    return personEntity
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity): PersonEntity {
    val updatedPerson = personService.updatePersonEntity(person, existingPersonEntity)
    updatedPerson.personKey?.let { reclusterService.recluster(it, changedRecord = updatedPerson) }
    return updatedPerson
  }
}
