package uk.gov.justice.digital.hmpps.personrecord.service.message

import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.exists
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class CreateUpdateService(
  private val personService: PersonService,
  private val publisher: ApplicationEventPublisher,
  private val reclusterService: ReclusterService,
) {

  @Transactional(isolation = REPEATABLE_READ)
  fun processPerson(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity = runBlocking {
    return@runBlocking findPerson().exists(
      no = {
        create(person)
      },
      yes = {
        update(person, it)
      },
    )
  }

  private fun create(person: Person): PersonEntity {
    val personEntity: PersonEntity = personService.createPersonEntity(person)
    publisher.publishEvent(PersonCreated(personEntity))
    return personEntity
  }

  private fun update(person: Person, existingPersonEntity: PersonEntity): PersonEntity {
    val personUpdated = personService.updatePersonEntity(person, existingPersonEntity)
    publisher.publishEvent(personUpdated)

    personUpdated.personEntity.personKey?.let {
      if (person.reclusterOnUpdate && personUpdated.matchingFieldsHaveChanged) {
        reclusterService.recluster(it, personUpdated.personEntity)
      }
    }
    return personUpdated.personEntity
  }
}
