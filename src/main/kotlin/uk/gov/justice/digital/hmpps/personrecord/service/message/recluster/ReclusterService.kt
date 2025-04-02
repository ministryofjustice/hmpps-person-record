package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse.Companion.result
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
    val matchesToChangeRecord: List<PersonMatchResult> = personMatchService.findHighestConfidencePersonRecordsByProbabilityDesc(changedRecord)
    val clusterDetails = ClusterDetails(cluster, changedRecord, matchesToChangeRecord)
    when {
      clusterDetails.relationship.isDifferent() -> handleDiscrepancyOfMatchesToExistingRecords(clusterDetails)
    }
  }

  private fun handleDiscrepancyOfMatchesToExistingRecords(clusterDetails: ClusterDetails) {
    when {
      clusterDetails.relationship.isSmaller() -> handleUnmatchedRecords(clusterDetails)
      else -> return // CPR-617 Handle more high quality matches
    }
  }

  private fun handleUnmatchedRecords(clusterDetails: ClusterDetails) {
    when {
      clusterDetails.matchedRecords.isEmpty() -> handleInvalidClusterComposition(clusterDetails.cluster)
      else -> personMatchService.examineIsClusterValid(clusterDetails.cluster).result(
        isValid = { }, // Will need to check if extra records to merge here
        isNotValid = {
          handleInvalidClusterComposition(clusterDetails.cluster)
        },
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

  private fun clusterNeedsAttention(personKeyEntity: PersonKeyEntity?) = personKeyEntity?.status == UUIDStatusType.NEEDS_ATTENTION
}
