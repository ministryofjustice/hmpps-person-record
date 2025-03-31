package uk.gov.justice.digital.hmpps.personrecord.service.message

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
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_NO_CHANGE

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
    val existingRecordsInCluster = cluster.personEntities.filterNot { it.id == changedRecord.id }
    val matchesToChangeRecord: List<PersonMatchResult> = personMatchService.findHighestConfidencePersonRecordsByProbabilityDesc(changedRecord)
    val matchedRecords: List<PersonEntity> = matchesToChangeRecord.map { it.personEntity }
    when {
      recordsDontMatch(matchedRecords, existingRecordsInCluster) -> handleDiscrepancyOfMatchesToExistingRecords(matchedRecords, existingRecordsInCluster, cluster)
      else -> logNoChangeToCluster(cluster)
    }
  }

  private fun handleDiscrepancyOfMatchesToExistingRecords(matchedRecords: List<PersonEntity>, existingRecordsInCluster: List<PersonEntity>, cluster: PersonKeyEntity) {
    when {
      hasLessMatchedRecordsInExistingCluster(matchedRecords, existingRecordsInCluster) -> handleLessMatchRecords(matchedRecords, cluster)
      else -> return // CPR-617 Handle more high quality matches
    }
  }

  private fun handleLessMatchRecords(matchedRecords: List<PersonEntity>, cluster: PersonKeyEntity) {
    when {
      matchedRecords.isEmpty() -> setClusterAsNeedsAttention(cluster)
      else -> personMatchService.examineIsClusterValid(cluster).result(
        isValid = { logNoChangeToCluster(cluster) },
        isNotValid = {
          // Need to evaluate what to log out here if anything / event log
          setClusterAsNeedsAttention(cluster)
        },
      )
    }
  }

  private fun recordsDontMatch(matchedRecords: List<PersonEntity>, existingRecordsInCluster: List<PersonEntity>): Boolean = matchedRecords.map { it.id }.toSet() != existingRecordsInCluster.map { it.id }.toSet()

  private fun hasLessMatchedRecordsInExistingCluster(matchedRecords: List<PersonEntity>, existingRecordsInCluster: List<PersonEntity>): Boolean
    = existingRecordsInCluster.map { it.id }.toSet().subtract(matchedRecords.map { it.id }.toSet()).isNotEmpty()

  private fun setClusterAsNeedsAttention(cluster: PersonKeyEntity) {
    cluster.status = UUIDStatusType.NEEDS_ATTENTION
    personKeyRepository.save(cluster)
    telemetryService.trackEvent(
      CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
      mapOf(EventKeys.UUID to cluster.personId.toString()),
    )
  }

  private fun logNoChangeToCluster(cluster: PersonKeyEntity) {
    telemetryService.trackEvent(CPR_RECLUSTER_NO_CHANGE, mapOf(EventKeys.UUID to cluster.personId.toString()))
  }

  private fun clusterNeedsAttention(personKeyEntity: PersonKeyEntity?) = personKeyEntity?.status == UUIDStatusType.NEEDS_ATTENTION
}
