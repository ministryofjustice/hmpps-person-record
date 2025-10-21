package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.review.ReviewRaised
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.unmerge.PersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.service.person.OverrideService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyService

@Component
class UnmergeService(
  private val personKeyService: PersonKeyService,
  private val publisher: ApplicationEventPublisher,
  private val overrideService: OverrideService,
) {

  fun processUnmerge(reactivated: PersonEntity, existing: PersonEntity) {
    unmerge(reactivated, existing)
    when {
      clusterContainsAdditionalRecords(existing) -> setClusterAsNeedsAttention(existing, reactivated)
    }
  }

  private fun unmerge(reactivated: PersonEntity, existing: PersonEntity) {
    reactivated.personKey?.let { reactivated.removePersonKeyLink() }
    reactivated.removeMergedLink()

    overrideService.systemExclude(reactivated, existing)

    personKeyService.linkRecordToPersonKey(reactivated)
    publisher.publishEvent(PersonUnmerged(reactivated, existing))
  }

  private fun setClusterAsNeedsAttention(existing: PersonEntity, reactivated: PersonEntity) {
    existing.personKey?.let {
      it.setAsNeedsAttention(UUIDStatusReasonType.OVERRIDE_CONFLICT)
      publisher.publishEvent(ReviewRaised(it, listOf(reactivated.personKey!!)))
    }
  }

  private fun clusterContainsAdditionalRecords(existing: PersonEntity): Boolean = existing.personKey?.personEntities?.let { it.size > 1 } ?: false
}
