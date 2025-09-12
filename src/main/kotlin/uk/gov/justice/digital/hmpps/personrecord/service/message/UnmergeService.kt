package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideScopeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OverrideScopeRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
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
  private val overrideScopeRepository: OverrideScopeRepository,
) {

  fun processUnmerge(reactivated: PersonEntity, existing: PersonEntity) {
    when {
      clusterContainsAdditionalRecords(reactivated, existing) -> setClusterAsNeedsAttention(existing)
    }
    unmerge(reactivated, existing)
  }

  private fun unmerge(reactivated: PersonEntity, existing: PersonEntity) {
    val scopeEntity: OverrideScopeEntity = overrideScopeRepository.save(
      OverrideScopeEntity.new(confidence = ConfidenceType.VERIFIED, actor = ActorType.SYSTEM),
    )

    existing.addExcludeOverrideMarker(excludeRecord = reactivated)
    existing.addOverrideMarker(scopeEntity)
    personMatchService.saveToPersonMatch(existing)

    reactivated.personKey?.let { reactivated.removePersonKeyLink() }
    reactivated.removeMergedLink()

    reactivated.addExcludeOverrideMarker(excludeRecord = existing)
    reactivated.addOverrideMarker(scopeEntity)
    personMatchService.saveToPersonMatch(reactivated)

    personService.linkRecordToPersonKey(reactivated)
    publisher.publishEvent(PersonUnmerged(reactivated, existing))
  }

  private fun setClusterAsNeedsAttention(existing: PersonEntity) {
    existing.personKey?.let {
      it.setAsNeedsAttention(UUIDStatusReasonType.OVERRIDE_CONFLICT)
      personKeyRepository.save(it)
      publisher.publishEvent(RecordEventLog.from(CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION, existing, it))
    }
  }

  private fun clusterContainsAdditionalRecords(reactivated: PersonEntity, existing: PersonEntity): Boolean = existing.personKey?.personEntities?.any {
    listOf(existing.id, reactivated.id).contains(it.id).not()
  } ?: false
}
