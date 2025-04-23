package uk.gov.justice.digital.hmpps.personrecord.service.person

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonService(
  private val personRepository: PersonRepository,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
  private val retryExecutor: RetryExecutor,
) {

  fun createPersonEntity(person: Person): PersonEntity {
    val personEntity = createNewPersonEntity(person)
    sendPersonDetailsToPersonMatch(personEntity)
    return personEntity
  }

  fun updatePersonEntity(person: Person, existingPersonEntity: PersonEntity): PersonEntity {
    val updatedEntity = updateExistingPersonEntity(person, existingPersonEntity)
    sendPersonDetailsToPersonMatch(updatedEntity)
    return updatedEntity
  }

  fun linkRecordToPersonKey(personEntity: PersonEntity): PersonEntity {
    val personEntityWithKey = personKeyService.getOrCreatePersonKey(personEntity)
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

  private fun sendPersonDetailsToPersonMatch(personEntity: PersonEntity) = runBlocking {
    retryExecutor.runWithRetryHTTP {
      personMatchService.saveToPersonMatch(personEntity)
    }
  }
}
