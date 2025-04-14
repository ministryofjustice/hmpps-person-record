package uk.gov.justice.digital.hmpps.personrecord.service.person

import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonAssignedToUUID
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated

@Component
class PersonService(
  private val personRepository: PersonRepository,
  private val personKeyService: PersonKeyService,
  private val personMatchClient: PersonMatchClient,
  private val retryExecutor: RetryExecutor,
  private val publisher: ApplicationEventPublisher,
) {

  fun createPersonEntity(person: Person): PersonEntity {
    val personEntity = createNewPersonEntity(person)
    sendPersonDetailsToPersonMatch(personEntity)
    publisher.publishEvent(PersonCreated(personEntity))
    return personEntity
  }

  fun updatePersonEntity(person: Person, existingPersonEntity: PersonEntity): PersonEntity {
    val updatedEntity = updateExistingPersonEntity(person, existingPersonEntity)
    sendPersonDetailsToPersonMatch(updatedEntity)
    publisher.publishEvent(PersonUpdated(updatedEntity))
    return updatedEntity
  }

  fun linkRecordToPersonKey(personEntity: PersonEntity): PersonEntity {
    val personKeyEntity = personKeyService.getOrCreatePersonKey(personEntity)
    personEntity.personKey = personKeyEntity
    publisher.publishEvent(PersonAssignedToUUID(personEntity))
    return personRepository.saveAndFlush(personEntity)
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
      personMatchClient.postPerson(PersonMatchRecord.from(personEntity))
    }
  }
}
