package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.recluster.Recluster
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class ReclusterEventListener(
  private val reclusterService: ReclusterService,
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val publisher: ApplicationEventPublisher,
) {

  @EventListener
  fun onRecluster(recluster: Recluster) {
    if (clusterNeedsAttentionAndIsInvalid(recluster.cluster)) {
      return
    }
    settingNeedsAttentionClusterToActive(recluster.cluster, recluster.changedRecord)
    reclusterService.recluster(recluster.cluster, recluster.changedRecord)
  }

  private fun settingNeedsAttentionClusterToActive(personKeyEntity: PersonKeyEntity, changedRecord: PersonEntity) {
    if (personKeyEntity.isNeedsAttention()) {
      personKeyEntity.status = ACTIVE
      personKeyRepository.save(personKeyEntity)
      publisher.publishEvent(
        RecordEventLog(
          CPRLogEvents.CPR_NEEDS_ATTENTION_TO_ACTIVE,
          changedRecord,
          personKeyEntity,
        ),
      )
    }
  }
  private fun clusterNeedsAttentionAndIsInvalid(cluster: PersonKeyEntity) = cluster.isNeedsAttention() && !personMatchService.examineIsClusterValid(cluster).isClusterValid
  private fun PersonKeyEntity.isNeedsAttention(): Boolean = this.status == NEEDS_ATTENTION
}
