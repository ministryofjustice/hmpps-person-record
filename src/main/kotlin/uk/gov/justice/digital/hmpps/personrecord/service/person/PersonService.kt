package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.updater.OtherPersonUpdater
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonProcessingCompleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import kotlin.reflect.KClass

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
    childrenToIgnore: Set<KClass<*>> = emptySet(),
    findPerson: () -> PersonEntity?,
  ): PersonEntity = findPerson().exists(
    no = {
      create(person, childrenToIgnore)
    },
    yes = {
      update(person, it, childrenToIgnore)
    },
  ).also {
    publisher.publishEvent(PersonProcessingCompleted(it))
  }

  private fun create(person: Person, childrenToIgnore: Set<KClass<*>> = emptySet()): PersonEntity {
    val personEntity = personRepository.save(PersonEntity.new(person, childrenToIgnore))
    personMatchService.saveToPersonMatch(personEntity)
    if (person.behaviour.linkOnCreate) {
      personKeyService.linkRecordToPersonKey(personEntity)
    }
    publisher.publishEvent(PersonCreated(personEntity))
    return personEntity
  }

  private fun update(person: Person, personEntity: PersonEntity, childrenToIgnore: Set<KClass<*>> = emptySet()): PersonEntity {
    val beforeUpdate = PersonMatchRecord.from(personEntity)
    personEntity.update(person, childrenToIgnore, OtherPersonUpdater())
    personRepository.save(personEntity)
    val matchingFieldsChanged = beforeUpdate.matchingFieldsAreDifferent(personEntity)
    if (matchingFieldsChanged && !personEntity.isPassive()) {
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
}
