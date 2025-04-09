package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse.Companion.result
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import kotlin.collections.groupBy

@Component
class ReclusterService(
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
) {

  fun recluster(cluster: PersonKeyEntity, changedRecord: PersonEntity) {
    when {
      clusterNeedsAttention(cluster) -> telemetryService.trackEvent(
        TelemetryEventType.CPR_RECLUSTER_UUID_MARKED_NEEDS_ATTENTION,
        mapOf(EventKeys.UUID to cluster.personId.toString()),
      )
      else -> handleRecluster(cluster, changedRecord)
    }
  }

  private fun handleRecluster(cluster: PersonKeyEntity, changedRecord: PersonEntity) {
    val matchesToChangeRecord: List<PersonMatchResult> = personMatchService.findHighestConfidencePersonRecordsByProbabilityDesc(changedRecord)
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

    matchedRecordsClusters.forEach {
      mergeClusters(it, clusterDetails.cluster)
    }
  }

  private fun mergeClusters(from: PersonKeyEntity, to: PersonKeyEntity) {
    from.mergedTo = to.id
    from.status = UUIDStatusType.RECLUSTER_MERGE
    personKeyRepository.save(from)

    from.personEntities.forEach { personEntity -> personEntity.personKey = to }
    personRepository.saveAll(from.personEntities)

    telemetryService.trackEvent(
      TelemetryEventType.CPR_RECLUSTER_MERGE,
      mapOf(
        EventKeys.FROM_UUID to from.personId.toString(),
        EventKeys.TO_UUID to to.personId.toString(),
      ),
    )
  }

  private fun handleUnmatchedRecords(clusterDetails: ClusterDetails) {
    when {
      clusterDetails.relationship.notMatchedToAnyRecord() -> handleInvalidClusterComposition(clusterDetails.cluster)
      else -> personMatchService.examineIsClusterValid(clusterDetails.cluster).result(
        isValid = { handleMergeClusters(clusterDetails) },
        isNotValid = { handleInvalidClusterComposition(clusterDetails.cluster) },
      )
    }
  }

  private fun handleInvalidClusterComposition(cluster: PersonKeyEntity) {
    telemetryService.trackEvent(
      TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
      mapOf(EventKeys.UUID to cluster.personId.toString()),
    )
    setClusterAsNeedsAttention(cluster)
  }

  private fun setClusterAsNeedsAttention(cluster: PersonKeyEntity) {
    cluster.status = UUIDStatusType.NEEDS_ATTENTION
    personKeyRepository.save(cluster)
  }

  private fun List<PersonKeyEntity>.removeUpdatedCluster(cluster: PersonKeyEntity) = this.filterNot { it.id == cluster.id }

  private fun List<PersonKeyEntity>.getActiveClusters() = this.filter { it.status == UUIDStatusType.ACTIVE }

  private fun List<PersonEntity>.collectDistinctClusters(): List<PersonKeyEntity> = this.groupBy { it.personKey!! }.map { it.key }.distinctBy { it.id }

  private fun clusterNeedsAttention(personKeyEntity: PersonKeyEntity?): Boolean = personKeyEntity?.status == UUIDStatusType.NEEDS_ATTENTION
}
