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
import java.time.LocalDateTime
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
      processUpdate(PersonLookup.ByEntity(it)) { personEntity ->
        personEntity.updatePersonEntity(person, childrenToIgnore)
      }
    },
  )

  @Transactional
  fun <T> processCreate(sourceSystem: SourceSystemType, callBack: (PersonEntity) -> T): T {
    val personEntity = personRepository.save(PersonEntity.new(sourceSystem))
    val value = callBack(personEntity)
    personRepository.saveAndFlush(personEntity)
    personMatchService.saveToPersonMatch(personEntity)
    personKeyService.linkRecordToPersonKey(personEntity)
    publisher.publishEvent(PersonCreated(personEntity))
    return value
  }

  @Transactional
  fun <T> processUpdate(personLookup: PersonLookup, callBack: (PersonEntity) -> T): T = processUpdate(personLookup) { personEntity, personChangeChecker ->
    val result = callBack(personEntity)
    publisher.publishEvent(PersonUpdated(personEntity, personChangeChecker))
    result
  }

  @Transactional
  fun <T> processUpdate(personLookup: PersonLookup, callBack: (PersonEntity, PersonChangeChecker) -> T): T {
    val personEntity = personLookup.find(personRepository)
    personEntity.lastModified = LocalDateTime.now()
    val personChangeChecker = PersonChangeChecker(personEntity)
    val value = callBack(personEntity, personChangeChecker)
    if (personChangeChecker.shouldSaveToPersonMatch(personEntity)) {
      personMatchService.saveToPersonMatch(personEntity)
      personEntity.personKey?.let { reclusterService.recluster(personEntity) }
    }
    return value
  }

  private fun PersonEntity?.exists(no: () -> PersonEntity, yes: (personEntity: PersonEntity) -> PersonEntity): PersonEntity = when {
    this == null -> no()
    else -> yes(this)
  }

  sealed interface PersonLookup {
    fun find(repo: PersonRepository): PersonEntity

    data class ByPrisonNumber(val prisonNumber: String) : PersonLookup {
      override fun find(repo: PersonRepository) = repo.findByPrisonNumber(prisonNumber)!!
    }

    data class ByCrn(val crn: String) : PersonLookup {
      override fun find(repo: PersonRepository) = repo.findByCrn(crn)!!
    }

    data class ByEntity(val personEntity: PersonEntity) : PersonLookup {
      override fun find(repo: PersonRepository): PersonEntity = repo.findById(personEntity.id!!).orElseThrow()
    }

    data class ById(val id: Long) : PersonLookup {
      override fun find(repo: PersonRepository): PersonEntity = repo.findById(id).orElseThrow()
    }
  }
}
