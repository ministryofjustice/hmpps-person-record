package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideMarkerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideScopeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ActorType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ConfidenceType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.unmerge.PersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class UnmergeService(
  private val personService: PersonService,
  private val personKeyRepository: PersonKeyRepository,
  private val personRepository: PersonRepository,
  private val publisher: ApplicationEventPublisher,
  private val personMatchService: PersonMatchService,
) {

  @Transactional
  fun processUnmerge(reactivated: PersonEntity, existing: PersonEntity) {
    when {
      clusterContainsAdditionalRecords(reactivated, existing) -> setClusterAsNeedsAttention(existing)
    }
    unmerge(reactivated, existing)
  }

  private fun unmerge(reactivated: PersonEntity, existing: PersonEntity) {
    val scope: OverrideScopeEntity = OverrideScopeEntity.new(confidence = ConfidenceType.VERIFIED, actor = ActorType.SYSTEM)

    existing.addExcludeOverrideMarker(excludeRecord = reactivated)
    existing.addOverrideMarker(scope)
    personRepository.save(existing)
    personMatchService.saveToPersonMatch(existing)

    reactivated.personKey?.let { reactivated.removePersonKeyLink() }
    reactivated.removeMergedLink()

    reactivated.addExcludeOverrideMarker(excludeRecord = existing)
    reactivated.addOverrideMarker(scope)
    personMatchService.saveToPersonMatch(reactivated)

    personService.linkRecordToPersonKey(reactivated)
    publisher.publishEvent(PersonUnmerged(reactivated, existing))
  }

  private fun setClusterAsNeedsAttention(existing: PersonEntity) {
    existing.personKey?.let {
      it.status = UUIDStatusType.NEEDS_ATTENTION
      personKeyRepository.save(it)
      publisher.publishEvent(RecordEventLog.from(CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION, existing, it))
    }
  }

  private fun clusterContainsAdditionalRecords(reactivated: PersonEntity, existing: PersonEntity): Boolean = existing.personKey?.personEntities?.any {
    listOf(existing.id, reactivated.id).contains(it.id).not()
  } ?: false
}
