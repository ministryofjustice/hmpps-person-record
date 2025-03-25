package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_NO_CHANGE

@Component
class ReclusterService(
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val telemetryService: TelemetryService,
) {

  fun recluster(cluster: PersonKeyEntity, changedRecord: PersonEntity) {
    val otherRecordsInCluster = cluster.personEntities.filterNot { it.id == changedRecord.id  }
    val matches: List<PersonMatchResult> = personMatchService.findHighestConfidencePersonRecordsByProbabilityDesc(changedRecord)
    val matchedRecords: List<PersonEntity> = matches.map { it.personEntity }
    when {
      doesNotMatchRecordsInCluster(matchedRecords, otherRecordsInCluster) -> setClusterAsNeedsAttention(cluster)
      else -> telemetryService.trackEvent(CPR_RECLUSTER_NO_CHANGE, mapOf(EventKeys.UUID to cluster.personId.toString()),)
    }
  }

  private fun doesNotMatchRecordsInCluster(matchedRecords: List<PersonEntity>, otherRecordsInCluster: List<PersonEntity>): Boolean = matchedRecords.containsAll(otherRecordsInCluster)

  private fun setClusterAsNeedsAttention(cluster: PersonKeyEntity) {
    cluster.status = UUIDStatusType.NEEDS_ATTENTION
    personKeyRepository.save(cluster)
    telemetryService.trackEvent(
      CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
      mapOf(EventKeys.UUID to cluster.personId.toString()),
    )
  }

}