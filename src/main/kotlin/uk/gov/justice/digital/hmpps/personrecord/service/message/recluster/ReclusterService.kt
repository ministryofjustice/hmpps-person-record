package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse.Companion.result
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.ValidCluster
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class ReclusterService(
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val objectMapper: ObjectMapper,
  private val telemetryService: TelemetryService,
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
    val existingRecordsInCluster = cluster.personEntities.filterNot { it.id == changedRecord.id }
    val matchesToChangeRecord: List<PersonMatchResult> = personMatchService.findHighestConfidencePersonRecordsByProbabilityDesc(changedRecord)
    val matchedRecords: List<PersonEntity> = matchesToChangeRecord.map { it.personEntity }
    val clusterRelationship = ClusterRelationship(matchedRecords, existingRecordsInCluster)
    when {
      clusterRelationship.isDifferent() -> handleDiscrepancyOfMatchesToExistingRecords(clusterRelationship, cluster, changedRecord)
    }
  }

  private fun handleDiscrepancyOfMatchesToExistingRecords(clusterRelationship: ClusterRelationship, cluster: PersonKeyEntity, changedRecord: PersonEntity) {
    when {
      clusterRelationship.clusterIsSmaller() -> handleUnmatchedRecords(clusterRelationship.matchedRecords, cluster, changedRecord)
      else -> return // CPR-617 Handle more high quality matches
    }
  }

  private fun handleUnmatchedRecords(matchedRecords: List<PersonEntity>, cluster: PersonKeyEntity, changedRecord: PersonEntity) {
    when {
      matchedRecords.isEmpty() -> handleSingleRecordDoesNotMatchAnyRecordInCluster(cluster, changedRecord)
      else -> personMatchService.examineIsClusterValid(cluster).result(
        isValid = { }, // Will need to check if extra records to merge here
        isNotValid = { clusterComposition ->
          handleInvalidClusterComposition(cluster, clusterComposition)
        },
      )
    }
  }

  private fun handleSingleRecordDoesNotMatchAnyRecordInCluster(cluster: PersonKeyEntity, changedRecord: PersonEntity) {
    handleInvalidClusterComposition(
      cluster,
      clusterComposition = listOf(
        ValidCluster(records = listOf(changedRecord.matchId.toString())),
        ValidCluster(records = cluster.personEntities.filterNot { it.id == changedRecord.id }.map { it.matchId.toString() }),
      ),
    )
  }

  private fun handleInvalidClusterComposition(cluster: PersonKeyEntity, clusterComposition: List<ValidCluster>) {
    telemetryService.trackEvent(
      TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
      mapOf(
        EventKeys.UUID to cluster.personId.toString(),
        EventKeys.CLUSTER_COMPOSITION to objectMapper.writeValueAsString(clusterComposition),
      ),
    )
    setClusterAsNeedsAttention(cluster)
  }

  private fun setClusterAsNeedsAttention(cluster: PersonKeyEntity) {
    cluster.status = UUIDStatusType.NEEDS_ATTENTION
    personKeyRepository.save(cluster)
  }

  private fun clusterNeedsAttention(personKeyEntity: PersonKeyEntity?) = personKeyEntity?.status == UUIDStatusType.NEEDS_ATTENTION
}
