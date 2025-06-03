package uk.gov.justice.digital.hmpps.personrecord.service.person

import jakarta.persistence.OptimisticLockException
import kotlinx.coroutines.runBlocking
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.exists
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonFactory
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonProcessorChain
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonService(
  private val personRepository: PersonRepository,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
  private val personFactory: PersonFactory,
) {

  @Retryable(
    backoff = Backoff(random = true, delay = 1000, maxDelay = 2000, multiplier = 1.5),
    retryFor = [
      OptimisticLockException::class,
      DataIntegrityViolationException::class,
      CannotAcquireLockException::class,
    ],
  )
  @Transactional(isolation = REPEATABLE_READ)
  fun processPerson(
    person: Person,
    processor: (PersonProcessorChain) -> PersonEntity,
  ): PersonEntity = runBlocking {
    processor(personFactory.from(person))
  }

  fun createPersonEntity(person: Person): PersonEntity {
    val personEntity = createNewPersonEntity(person)
    personMatchService.saveToPersonMatch(personEntity)
    return personEntity
  }

  fun updatePersonEntity(person: Person, existingPersonEntity: PersonEntity, shouldReclusterOnUpdate: Boolean): PersonUpdated {
    val oldMatchingDetails = PersonMatchRecord.from(existingPersonEntity)
    val updatedEntity = updateExistingPersonEntity(person, existingPersonEntity)
    val matchingFieldsHaveChanged = oldMatchingDetails.matchingFieldsAreDifferent(
      PersonMatchRecord.from(
        updatedEntity,
      ),
    )

    when {
      matchingFieldsHaveChanged -> personMatchService.saveToPersonMatch(updatedEntity)
    }

    val shouldRecluster = when (personKeyService.clusterNeedsAttentionAndIsInvalid(updatedEntity.personKey)) {
      true -> false
      else -> {
        personKeyService.settingNeedsAttentionClusterToActive(updatedEntity.personKey, updatedEntity)
        shouldReclusterOnUpdate
      }
    }
    return PersonUpdated(updatedEntity, matchingFieldsHaveChanged, shouldRecluster)
  }

  fun linkRecordToPersonKey(personEntity: PersonEntity): PersonEntity {
    val personEntityWithKey = personMatchService.findHighestConfidencePersonRecord(personEntity).exists(
      no = { personKeyService.createPersonKey(personEntity) },
      yes = { personKeyService.retrievePersonKey(personEntity, it) },
    )
    return personRepository.saveAndFlush(personEntityWithKey)
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    personEntity.update(person)
    return personRepository.save(personEntity)
  }

  private fun createNewPersonEntity(person: Person): PersonEntity {
    val personEntity = PersonEntity.new(person)
    return personRepository.save(personEntity)
  }
}
