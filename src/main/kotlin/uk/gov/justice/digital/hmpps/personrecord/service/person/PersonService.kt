package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker
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
  @Transactional
  fun processPerson(
    person: Person,
    childrenToIgnore: Set<KClass<*>> = emptySet(),
    findPerson: () -> PersonEntity?,
  ): PersonEntity = findPerson().exists(
    no = {
      create(person, childrenToIgnore)
    },
    yes = {
      val (personEntity, personChangeChecker) = update(person, it, childrenToIgnore)
      publisher.publishEvent(PersonUpdated(personEntity, personChangeChecker))
      personEntity
    },
  ).also {
    publisher.publishEvent(PersonProcessingCompleted(it))
  }

  private fun create(person: Person, childrenToIgnore: Set<KClass<*>>): PersonEntity {
    val personEntity = PersonEntity.new(person.sourceSystem).updatePersonEntity(person, childrenToIgnore)
    personRepository.save(personEntity)
    personMatchService.saveToPersonMatch(personEntity)
    personKeyService.linkRecordToPersonKey(personEntity)
    publisher.publishEvent(PersonCreated(personEntity))
    return personEntity
  }

  fun update(person: Person, personEntity: PersonEntity, childrenToIgnore: Set<KClass<*>> = emptySet()): Pair<PersonEntity, PersonChangeChecker> {
    val personChangeChecker = PersonChangeChecker(personEntity)
    personEntity.updatePersonEntity(person, childrenToIgnore)
    personRepository.save(personEntity)

    if (personChangeChecker.shouldSaveToPersonMatch(personEntity)) {
      personMatchService.saveToPersonMatch(personEntity)
      personEntity.personKey?.let { reclusterService.recluster(personEntity) }
    }
    return personEntity to personChangeChecker
  }

  private fun PersonEntity?.exists(no: () -> PersonEntity, yes: (personEntity: PersonEntity) -> PersonEntity): PersonEntity = when {
    this == null -> no()
    else -> yes(this)
  }
}
