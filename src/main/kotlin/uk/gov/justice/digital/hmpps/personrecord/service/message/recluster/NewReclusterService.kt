package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.BROKEN_CLUSTER
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class NewReclusterService(
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val publisher: ApplicationEventPublisher
) {

  @Transactional
  fun recluster(changedRecord: PersonEntity) {
    val cluster = changedRecord.personKey!!
    when {
      cluster.clusterIsBrokenAndCanBecomeActive() -> settingNeedsAttentionClusterToActive(cluster, changedRecord)
    }
    when {
      cluster.isActive() -> {}
    }
  }

  private fun settingNeedsAttentionClusterToActive(personKeyEntity: PersonKeyEntity, changedRecord: PersonEntity) {
    personKeyEntity.setAsActive()
    personKeyRepository.save(personKeyEntity)
    publisher.publishEvent(
      RecordEventLog.from(
        CPRLogEvents.CPR_NEEDS_ATTENTION_TO_ACTIVE,
        changedRecord,
        personKeyEntity,
      ),
    )
  }

  private fun PersonKeyEntity.clusterIsBrokenAndCanBecomeActive() = this.isNeedsAttention(BROKEN_CLUSTER) && this.clusterIsValid()

  private fun PersonKeyEntity.clusterIsValid() = if (this.hasOneRecord()) true else personMatchService.examineIsClusterValid(this).isClusterValid

  private fun PersonKeyEntity.hasOneRecord() = this.personEntities.size == 1
}