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
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.PersonChainable
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
    val personEntity = PersonChainable(
      personEntity = personRepository.save(PersonEntity.new(person)),
      matchingFieldsChanged = true,
      linkOnCreate = person.behaviour.linkOnCreate,
    )
      .saveToPersonMatch()
      .linkToPersonKey().personEntity
    publisher.publishEvent(PersonCreated(personEntity))
    return personEntity
  }

  private fun update(person: Person, personEntity: PersonEntity): PersonEntity {
    val beforeUpdate = PersonMatchRecord.from(personEntity)
    personEntity.update(person)
    val afterUpdate = PersonMatchRecord.from(personEntity)
    val matchingFieldsChanged = beforeUpdate.matchingFieldsAreDifferent(afterUpdate)
    PersonChainable(
      personEntity = personRepository.save(personEntity),
      matchingFieldsChanged = matchingFieldsChanged,
      linkOnCreate = person.behaviour.linkOnCreate,
    )
      .saveToPersonMatch()
      .reclusterIf { ctx -> person.behaviour.reclusterOnUpdate && ctx.matchingFieldsChanged }
    publisher.publishEvent(PersonUpdated(personEntity, matchingFieldsChanged))
    return personEntity
  }

  private fun PersonChainable.saveToPersonMatch(): PersonChainable {
    when {
      this.matchingFieldsChanged -> personMatchService.saveToPersonMatch(this.personEntity)
    }
    return this
  }

  private fun PersonChainable.linkToPersonKey(): PersonChainable {
    when {
      this.linkOnCreate -> personKeyService.linkRecordToPersonKey(this.personEntity)
    }
    return this
  }

  private fun PersonChainable.reclusterIf(condition: (ctx: PersonChainable) -> Boolean): PersonChainable {
    when {
      condition(this) -> this.personEntity.personKey?.let { reclusterService.recluster(this.personEntity) }
    }
    return this
  }

  private fun PersonEntity?.exists(no: () -> PersonEntity, yes: (personEntity: PersonEntity) -> PersonEntity): PersonEntity = when {
    this == null -> no()
    else -> yes(this)
  }
}
