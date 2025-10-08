package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.unmerge.PersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.service.person.OverrideService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyService

@Component
class UnmergeService(
  private val personKeyService: PersonKeyService,
  private val personKeyRepository: PersonKeyRepository,
  private val publisher: ApplicationEventPublisher,
  private val overrideService: OverrideService,
) {

  fun processUnmerge(reactivated: PersonEntity, existing: PersonEntity) {
    when {
      clusterContainsAdditionalRecords(reactivated, existing) -> setClusterAsNeedsAttention(existing)
    }
    unmerge(reactivated, existing)
  }

  private fun unmerge(reactivated: PersonEntity, existing: PersonEntity) {
    reactivated.personKey?.let { reactivated.removePersonKeyLink() }
    reactivated.removeMergedLink()

    overrideService.systemExclude(reactivated, existing)

    personKeyService.linkRecordToPersonKey(reactivated)
    publisher.publishEvent(PersonUnmerged(reactivated, existing))
  }

  private fun setClusterAsNeedsAttention(existing: PersonEntity) {
    existing.personKey?.let {
      it.setAsNeedsAttention(UUIDStatusReasonType.OVERRIDE_CONFLICT)
      personKeyRepository.save(it)
    }
  }

  private fun clusterContainsAdditionalRecords(reactivated: PersonEntity, existing: PersonEntity): Boolean = existing.personKey?.personEntities?.any {
    listOf(existing.id, reactivated.id).contains(it.id).not()
  } ?: false
}
