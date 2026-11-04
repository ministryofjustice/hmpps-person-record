package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker
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
      processCreate(PersonEntity.new(person.sourceSystem)) { personEntity ->
        personEntity.updatePersonEntity(person, childrenToIgnore)
      }
    },
    yes = {
      processUpdate(it) { personEntity ->
        personEntity.updatePersonEntity(person, childrenToIgnore)
      }
    },
  )

  @Transactional
  fun processCreate(new: PersonEntity, callBack: (PersonEntity) -> PersonEntity): PersonEntity {
    val edited = callBack(new)
    personRepository.save(edited)
    personMatchService.saveToPersonMatch(edited)
    personKeyService.linkRecordToPersonKey(edited)
    publisher.publishEvent(PersonCreated(edited))
    return edited
  }

  @Transactional
  fun <T> processUpdate(original: PersonEntity, callBack: (PersonEntity) -> T): T {
    val personChangeChecker = PersonChangeChecker(original)
    val value = callBack(original)
    val edited = personRepository.findById(original.id!!).get()
    personRepository.save(edited)
    if (personChangeChecker.shouldSaveToPersonMatch(edited)) {
      personMatchService.saveToPersonMatch(edited)
      edited.personKey?.let { reclusterService.recluster(edited) }
    }
    publisher.publishEvent(PersonUpdated(edited, personChangeChecker))
    return value
  }

  private fun PersonEntity?.exists(no: () -> PersonEntity, yes: (personEntity: PersonEntity) -> PersonEntity): PersonEntity = when {
    this == null -> no()
    else -> yes(this)
  }
}
