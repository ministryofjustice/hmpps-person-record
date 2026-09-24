package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.OverrideConflict
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.unmerge.PersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.OverrideService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyService
import uk.gov.justice.digital.hmpps.personrecord.service.person.updatePersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class UnmergeService(
  private val personKeyService: PersonKeyService,
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
  private val publisher: ApplicationEventPublisher,
  private val overrideService: OverrideService,
) {

  fun processUnmerge(reactivated: Person, existing: Person) {
    val existingPersonEntity = personRepository.findByCrn(existing.crn!!)!!
      .also { updatePerson(existing, it) }

    val reactivatedPersonEntity = personRepository.findByCrn(reactivated.crn!!)
      ?.let { updatePerson(reactivated, it) }
      ?: createPerson(reactivated)

    unmerge(reactivatedPersonEntity, existingPersonEntity)
    when {
      clusterContainsAdditionalRecords(existingPersonEntity) -> raiseForReview(existingPersonEntity, reactivatedPersonEntity)
    }
  }

  private fun createPerson(person: Person): PersonEntity {
    val personEntity = PersonEntity.new(person.sourceSystem).updatePersonEntity(person)
    personRepository.save(personEntity)

    personMatchService.saveToPersonMatch(personEntity)
    publisher.publishEvent(PersonCreated(personEntity))
    return personEntity
  }

  private fun updatePerson(person: Person, personEntity: PersonEntity): PersonEntity {
    val personChangeChecker = PersonChangeChecker(personEntity)
    personEntity.updatePersonEntity(person)
    personRepository.save(personEntity)

    if (personChangeChecker.matchingFieldsHaveChanged(personEntity) && !personEntity.isPassive()) {
      personMatchService.saveToPersonMatch(personEntity)
      personEntity.personKey?.let { reclusterService.recluster(personEntity) }
    }
    publisher.publishEvent(PersonUpdated(personEntity, personChangeChecker))
    return personEntity
  }

  private fun unmerge(reactivated: PersonEntity, existing: PersonEntity) {
    reactivated.removeMergedLink()
    overrideService.systemExclude(reactivated, existing)

    personKeyService.linkRecordToPersonKey(reactivated)
    publisher.publishEvent(PersonUnmerged(reactivated, existing))
  }

  private fun raiseForReview(existing: PersonEntity, reactivated: PersonEntity) {
    existing.personKey?.let {
      it.setAsNeedsAttention(UUIDStatusReasonType.OVERRIDE_CONFLICT)
      publisher.publishEvent(OverrideConflict(it, listOf(reactivated.personKey!!)))
    }
  }

  private fun clusterContainsAdditionalRecords(existing: PersonEntity): Boolean = existing.personKey?.personEntities?.let { it.size > 1 } ?: false
}
