package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.CircularMergeException
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse.Companion.result
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.ValidCluster
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.BROKEN_CLUSTER
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.OVERRIDE_CONFLICT
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.RECLUSTER_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class ReclusterService(
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val publisher: ApplicationEventPublisher,
  private val personRepository: PersonRepository,
) {

  @Transactional
  fun recluster(changedRecord: PersonEntity) {
    val cluster = changedRecord.personKey!!
    when {
      cluster.clusterIsBrokenAndCanBecomeActive() -> settingNeedsAttentionClusterToActive(cluster, changedRecord)
    }
    when {
      cluster.isActive() -> processRecluster(cluster, changedRecord)
    }
  }

  private fun processRecluster(cluster: PersonKeyEntity, changedRecord: PersonEntity) {
    val matchesToChangedRecord: List<PersonMatchResult> =
      personMatchService.findPersonRecordsAboveFractureThresholdByMatchWeightDesc(changedRecord)
    val clusterDetails = ClusterDetails(cluster, changedRecord, matchesToChangedRecord)
    when {
      clusterDetails.relationship.isDifferent() -> handleClusterChange(clusterDetails)
    }
  }

  private fun handleClusterChange(clusterDetails: ClusterDetails) {
    when {
      clusterDetails.relationship.isSmaller() -> handleUnmatchedRecords(clusterDetails)
      else -> handleMergeClusters(clusterDetails)
    }
  }

  private fun handleUnmatchedRecords(clusterDetails: ClusterDetails) {
    when {
      clusterDetails.relationship.notMatchedToAnyRecord() -> handleInvalidClusterComposition(clusterDetails)
      else -> personMatchService.examineIsClusterValid(clusterDetails.cluster).result(
        isValid = { handleMergeClusters(clusterDetails) },
        isNotValid = { clusterComposition -> handleInvalidClusterComposition(clusterDetails, clusterComposition) },
      )
    }
  }

  private fun handleMergeClusters(clusterDetails: ClusterDetails) {
    val matchedRecordsClusters: List<PersonKeyEntity> =
      clusterDetails.shouldJoinRecords
        .collectDistinctClusters()
        .removeUpdatedCluster(clusterDetails.cluster)
        .getActiveClusters()
    when {
      matchedRecordsClusters.isNotEmpty() -> checkCombinedClustersIsAValidClusterAndMerge(matchedRecordsClusters, clusterDetails)
    }
  }

  private fun checkCombinedClustersIsAValidClusterAndMerge(matchedRecordsClusters: List<PersonKeyEntity>, clusterDetails: ClusterDetails) {
    personMatchService.examineIsClusterMergeValid(clusterDetails.cluster, matchedRecordsClusters).result(
      isValid = {
        matchedRecordsClusters.forEach {
          mergeClusters(it, clusterDetails.cluster)
        }
      },
      isNotValid = { clusterComposition -> handleExclusionsBetweenMatchedClusters(clusterDetails, clusterComposition) },
    )
  }

  private fun handleInvalidClusterComposition(
    clusterDetails: ClusterDetails,
    clusterComposition: List<ValidCluster>? = null,
  ) {
    setToNeedsAttention(clusterDetails.cluster, reason = BROKEN_CLUSTER)
    publisher.publishEvent(
      RecordClusterTelemetry(
        TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        clusterDetails.cluster,
      ),
    )
    publisher.publishEvent(
      RecordEventLog.from(
        CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION,
        clusterDetails.changedRecord,
        clusterDetails.cluster,
        clusterComposition,
      ),
    )
  }

  private fun handleExclusionsBetweenMatchedClusters(clusterDetails: ClusterDetails, clusterComposition: List<ValidCluster>) {
    setToNeedsAttention(clusterDetails.cluster, reason = OVERRIDE_CONFLICT)
    publisher.publishEvent(
      RecordClusterTelemetry(
        TelemetryEventType.CPR_RECLUSTER_MATCHED_CLUSTERS_HAS_EXCLUSIONS,
        clusterDetails.cluster,
      ),
    )
    publisher.publishEvent(
      RecordEventLog.from(
        CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION,
        clusterDetails.changedRecord,
        clusterDetails.cluster,
        clusterComposition,
      ),
    )
  }

  private fun mergeClusters(from: PersonKeyEntity, to: PersonKeyEntity) {
    if (to.mergedTo == from.id) {
      throw CircularMergeException()
    }
    from.mergedTo = to.id
    from.status = RECLUSTER_MERGE
    personKeyRepository.save(from)
    publisher.publishEvent(RecordEventLog.from(CPRLogEvents.CPR_RECLUSTER_UUID_MERGED, from.personEntities.first(), from))

    from.personEntities.forEach { personEntity ->
      personEntity.personKey = to
      publisher.publishEvent(RecordEventLog.from(CPRLogEvents.CPR_RECLUSTER_RECORD_MERGED, personEntity))
    }
    personRepository.saveAll(from.personEntities)

    publisher.publishEvent(
      RecordTelemetry(
        TelemetryEventType.CPR_RECLUSTER_MERGE,
        mapOf(
          EventKeys.FROM_UUID to from.personUUID.toString(),
          EventKeys.TO_UUID to to.personUUID.toString(),
        ),
      ),
    )
  }

  private fun setToNeedsAttention(personKeyEntity: PersonKeyEntity, reason: UUIDStatusReasonType) {
    personKeyEntity.setAsNeedsAttention(reason)
    personKeyRepository.save(personKeyEntity)
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

  private fun List<PersonKeyEntity>.removeUpdatedCluster(cluster: PersonKeyEntity) = this.filterNot { it.id == cluster.id }

  private fun List<PersonKeyEntity>.getActiveClusters() = this.filter { it.status == ACTIVE }

  private fun List<PersonEntity>.collectDistinctClusters(): List<PersonKeyEntity> = this.groupBy { it.personKey!! }.map { it.key }.distinctBy { it.id }
}
