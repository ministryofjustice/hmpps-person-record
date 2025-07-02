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
        handlePersonCreation(person)
      },
      yes = {
        handlePersonUpdate(person, it)
      },
    )
  }

  private fun handlePersonCreation(person: Person): PersonEntity {
    val personEntity: PersonEntity = personService.createPersonEntity(person)
    if (person.linkOnCreate) {
      personService.linkRecordToPersonKey(personEntity)
    }
    publisher.publishEvent(PersonCreated(personEntity))
    return personEntity
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity): PersonEntity {
    val personUpdated = personService.updatePersonEntity(person, existingPersonEntity)
    publisher.publishEvent(personUpdated)

    val shouldRecluster = person.reclusterOnUpdate && personUpdated.matchingFieldsHaveChanged
    if (shouldRecluster) {
      personUpdated.personEntity.personKey?.let {
        reclusterService.recluster(it, personUpdated.personEntity)
      }
    }
    return personUpdated.personEntity
  }
}
