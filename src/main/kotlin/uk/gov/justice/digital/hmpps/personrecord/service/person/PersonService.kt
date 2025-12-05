package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonProcessingCompleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonService(
  private val personRepository: PersonRepository,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  fun processPerson(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity = findPerson().exists(
    no = {
      create(person)
    },
    yes = {
      update(person, it)
    },
  ).also {
    publisher.publishEvent(PersonProcessingCompleted(it))
  }

  private fun create(person: Person): PersonEntity {
    val personEntity = personRepository.save(PersonEntity.new(person))
    personMatchService.saveToPersonMatch(personEntity)
    if (person.behaviour.linkOnCreate) {
      personKeyService.linkRecordToPersonKey(personEntity)
    }
    publisher.publishEvent(PersonCreated(personEntity))
    return personEntity
  }

  private fun update(person: Person, personEntity: PersonEntity): PersonEntity {
    val beforeUpdate = PersonMatchRecord.from(personEntity)
    personEntity.update(person)
    val matchingFieldsChanged = beforeUpdate.matchingFieldsAreDifferent(personEntity)
    personRepository.save(personEntity)
    if (matchingFieldsChanged) {
      personMatchService.saveToPersonMatch(personEntity)
      recluster(person, personEntity)
    }
    publisher.publishEvent(PersonUpdated(personEntity, matchingFieldsChanged))
    return personEntity
  }

  private fun recluster(
    person: Person,
    personEntity: PersonEntity,
  ) {
    if (person.behaviour.reclusterOnUpdate) {
      personEntity.personKey?.let { reclusterService.recluster(personEntity) }
    }
  }

  private fun PersonEntity?.exists(no: () -> PersonEntity, yes: (personEntity: PersonEntity) -> PersonEntity): PersonEntity = when {
    this == null -> no()
    else -> yes(this)
  }

  private fun PersonMatchRecord.matchingFieldsAreDifferent(personEntity: PersonEntity): Boolean = this != PersonMatchRecord.from(personEntity)
}
