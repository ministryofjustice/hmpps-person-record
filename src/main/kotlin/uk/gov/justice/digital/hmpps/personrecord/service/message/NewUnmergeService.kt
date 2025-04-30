package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.unmerge.PersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class NewUnmergeService(
  private val personService: PersonService,
  private val personKeyRepository: PersonKeyRepository,
  private val personRepository: PersonRepository,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun processUnmerge(reactivated: PersonEntity, unmerged: PersonEntity) {
    when {
      clusterContainsAdditionalRecords(reactivated, unmerged) -> setClusterAsNeedsAttention(unmerged)
      else -> unmerge(reactivated, unmerged)
    }
  }

  private fun unmerge(reactivated: PersonEntity, unmerged: PersonEntity) {
    unmerged.addExcludeOverrideMarker(excludeRecord = reactivated)
    personRepository.save(unmerged)

    reactivated.personKey?.let { reactivated.removePersonKeyLink() }
    reactivated.removeMergedLink()

    reactivated.addExcludeOverrideMarker(excludeRecord = unmerged)
    personService.linkRecordToPersonKey(reactivated)
    publisher.publishEvent(PersonUnmerged(reactivated, unmerged))
  }

  private fun setClusterAsNeedsAttention(unmerged: PersonEntity) {
    unmerged.personKey?.let {
      it.status = UUIDStatusType.NEEDS_ATTENTION
      personKeyRepository.save(it)
      publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION, unmerged, it))
    }
  }

  private fun clusterContainsAdditionalRecords(reactivated: PersonEntity, unmerged: PersonEntity): Boolean {
    val additionalRecords = unmerged.personKey?.let { cluster ->
      cluster.personEntities.filter {
        listOf(unmerged.id, reactivated.id).contains(it.id).not()
      }
    }
    return (additionalRecords?.size ?: 0) > 0
  }
}
