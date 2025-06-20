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
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.RECLUSTER_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class ReclusterService(
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val personRepository: PersonRepository,
  private val publisher: ApplicationEventPublisher,
  private val personKeyService: PersonKeyService,
) {

  @Transactional
  fun recluster(cluster: PersonKeyEntity, changedRecord: PersonEntity) {
    when {
      hasClusterSetBackToActive(changedRecord) -> processRecluster(cluster, changedRecord)
    }
  }

  private fun processRecluster(cluster: PersonKeyEntity, changedRecord: PersonEntity) {
    val matchesToChangeRecord: List<PersonMatchResult> =
      personMatchService.findHighestConfidencePersonRecordsByProbabilityDesc(changedRecord)
    val clusterDetails = ClusterDetails(cluster, changedRecord, matchesToChangeRecord)
    when {
      clusterDetails.relationship.isDifferent() -> handleDiscrepancyOfMatchesToExistingRecords(clusterDetails)
    }
  }

  private fun handleDiscrepancyOfMatchesToExistingRecords(clusterDetails: ClusterDetails) {
    when {
      clusterDetails.relationship.isSmaller() -> handleUnmatchedRecords(clusterDetails)
      else -> handleMergeClusters(clusterDetails)
    }
  }

  private fun handleMergeClusters(clusterDetails: ClusterDetails) {
    val matchedRecordsClusters: List<PersonKeyEntity> =
      clusterDetails.matchedRecords
        .collectDistinctClusters()
        .removeUpdatedCluster(cluster = clusterDetails.cluster)
        .getActiveClusters()

    when {
      hasExcludeMarkerBetweenClusters(matchedRecordsClusters) -> handleExclusionsBetweenMatchedClusters(clusterDetails)
      else -> matchedRecordsClusters.forEach {
        mergeClusters(it, clusterDetails.cluster)
      }
    }
  }

  private fun hasExcludeMarkerBetweenClusters(clusters: List<PersonKeyEntity>): Boolean {
    val allRecordsIdsFromClusters = clusters.map { it.getRecordIds() }.flatten().toSet()
    return clusters.any { cluster ->
      val excludeMarkerRecordIds = cluster.collectExcludeOverrideMarkers().map { it.markerValue }.toSet()
      allRecordsIdsFromClusters.intersect(excludeMarkerRecordIds).isNotEmpty()
    }
  }

  private fun mergeClusters(from: PersonKeyEntity, to: PersonKeyEntity) {
    if (to.mergedTo == from.id) {
      throw CircularMergeException()
    }
    from.mergedTo = to.id
    from.status = RECLUSTER_MERGE
    personKeyRepository.save(from)
    publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECLUSTER_UUID_MERGED, from.personEntities.first(), from))

    from.personEntities.forEach { personEntity ->
      personEntity.personKey = to
      publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECLUSTER_RECORD_MERGED, personEntity))
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

  private fun handleUnmatchedRecords(clusterDetails: ClusterDetails) {
    when {
      clusterDetails.relationship.notMatchedToAnyRecord() -> handleInvalidClusterComposition(clusterDetails)
      else -> personMatchService.examineIsClusterValid(clusterDetails.cluster).result(
        isValid = { handleMergeClusters(clusterDetails) },
        isNotValid = { clusterComposition -> handleInvalidClusterComposition(clusterDetails, clusterComposition) },
      )
    }
  }

  private fun handleExclusionsBetweenMatchedClusters(clusterDetails: ClusterDetails) {
    publisher.publishEvent(
      RecordClusterTelemetry(
        TelemetryEventType.CPR_RECLUSTER_MATCHED_CLUSTERS_HAS_EXCLUSIONS,
        clusterDetails.cluster,
      ),
    )
    publisher.publishEvent(
      RecordEventLog(
        CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION,
        clusterDetails.changedRecord,
        clusterDetails.cluster,
      ),
    )
    settingClusterToNeedsAttention(clusterDetails)
  }

  private fun handleInvalidClusterComposition(
    clusterDetails: ClusterDetails,
    clusterComposition: List<ValidCluster>? = null,
  ) {
    publisher.publishEvent(
      RecordClusterTelemetry(
        TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        clusterDetails.cluster,
      ),
    )
    publisher.publishEvent(
      RecordEventLog(
        CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION,
        clusterDetails.changedRecord,
        clusterDetails.cluster,
        clusterComposition,
      ),
    )
    settingClusterToNeedsAttention(clusterDetails)
  }

  private fun settingClusterToNeedsAttention(clusterDetails: ClusterDetails) {
    clusterDetails.cluster.status = NEEDS_ATTENTION
    personKeyRepository.save(clusterDetails.cluster)
  }

  fun hasClusterSetBackToActive(updatedEntity: PersonEntity) = when (personKeyService.clusterNeedsAttentionAndIsInvalid(updatedEntity.personKey)) {
    true -> false
    else -> {
      personKeyService.settingNeedsAttentionClusterToActive(updatedEntity.personKey, updatedEntity)
      true
    }
  }

  private fun List<PersonKeyEntity>.removeUpdatedCluster(cluster: PersonKeyEntity) = this.filterNot { it.id == cluster.id }

  private fun List<PersonKeyEntity>.getActiveClusters() = this.filter { it.status == ACTIVE }

  private fun List<PersonEntity>.collectDistinctClusters(): List<PersonKeyEntity> = this.groupBy { it.personKey!! }.map { it.key }.distinctBy { it.id }
}
