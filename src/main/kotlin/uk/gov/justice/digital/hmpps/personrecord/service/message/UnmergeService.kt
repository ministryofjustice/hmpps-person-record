package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.OverrideConflict
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.unmerge.PersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.service.person.OverrideService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class UnmergeService(
  private val personKeyService: PersonKeyService,
  private val personRepository: PersonRepository,
  private val personService: PersonService,
  private val publisher: ApplicationEventPublisher,
  private val overrideService: OverrideService,
) {

  fun processUnmerge(reactivated: Person, existing: Person) {
    val existingPersonEntity = personRepository.findByCrn(existing.crn!!)!!
      .also {
        val (personEntity, personChangeChecker) = personService.update(existing, it)
        // TODO: remove once sas don't listen to update events for an unmerge anymore
        publisher.publishEvent(PersonUpdated(personEntity, personChangeChecker))
      }

    val reactivatedPersonEntity = personRepository.findByCrn(reactivated.crn!!)!!
      .also {
        val (personEntity, personChangeChecker) = personService.update(reactivated, it)
        // TODO: remove once sas don't listen to update events for an unmerge anymore
        publisher.publishEvent(PersonUpdated(personEntity, personChangeChecker))
      }

    unmerge(reactivatedPersonEntity, existingPersonEntity)
    when {
      clusterContainsAdditionalRecords(existingPersonEntity) -> raiseForReview(existingPersonEntity, reactivatedPersonEntity)
    }
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
