package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
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
      processCreate(person.sourceSystem) { personEntity ->
        personEntity.updatePersonEntity(person, childrenToIgnore)
      }
    },
    yes = {
      update(person, it, childrenToIgnore)
    },
  )

  @Transactional
  fun update(person: Person, personEntity: PersonEntity, childrenToIgnore: Set<KClass<*>> = emptySet()) = processUpdate(personEntity) { personEntity ->
    personEntity.updatePersonEntity(person, childrenToIgnore)
  }

  @Transactional
  fun <T> processCreate(sourceSystem: SourceSystemType, callBack: (PersonEntity) -> T): T {
    val personEntity = PersonEntity.new(sourceSystem)
    val value = callBack(personEntity)
    personRepository.save(personEntity)
    personMatchService.saveToPersonMatch(personEntity)
    personKeyService.linkRecordToPersonKey(personEntity)
    publisher.publishEvent(PersonCreated(personEntity))
    return value
  }

  @Transactional
  fun <T> processUpdate(personEntity: PersonEntity, callBack: (PersonEntity) -> T): T {
    val personChangeChecker = PersonChangeChecker(personEntity)
    val value = callBack(personEntity)
    if (personChangeChecker.shouldSaveToPersonMatch(personEntity)) {
      personMatchService.saveToPersonMatch(personEntity)
      personEntity.personKey?.let { reclusterService.recluster(personEntity) }
    }
    publisher.publishEvent(PersonUpdated(personEntity, personChangeChecker))
    return value
  }

  private fun PersonEntity?.exists(no: () -> PersonEntity, yes: (personEntity: PersonEntity) -> PersonEntity): PersonEntity = when {
    this == null -> no()
    else -> yes(this)
  }
}
