package uk.gov.justice.digital.hmpps.personrecord.service.person

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.search.MatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_RECLUSTER_NEEDS_ATTENTION

@Component
class ReclusterService(
  private val matchService: MatchService,
  private val telemetryService: TelemetryService,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  fun recluster(personKeyEntity: PersonKeyEntity) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      handleRecluster(personKeyEntity)
    }
  }

  private fun handleRecluster(personKeyEntity: PersonKeyEntity) {
    when {
      clusterNeedsAttention(personKeyEntity) -> telemetryService.trackEvent(
        CPR_UUID_RECLUSTER_NEEDS_ATTENTION,
        mapOf(
          EventKeys.UUID to personKeyEntity.personId.toString(),
        ),
      )
      clusterHasOneRecord(personKeyEntity) -> {} // CPR-437
      else -> handleMultipleRecordsInCluster(personKeyEntity)
    }
  }

  private fun handleMultipleRecordsInCluster(personKeyEntity: PersonKeyEntity) {
    val recordToCheck = personKeyEntity.personEntities.first()
    val recordsToMatch = personKeyEntity.personEntities.drop(1)
    val (matchedRecords, unmatchedRecords) = matchRecordAgainstCluster(recordToCheck, recordsToMatch)
    println(matchedRecords)
  }

  private fun matchRecordAgainstCluster(personEntity: PersonEntity, personEntities: List<PersonEntity>): Pair<List<PersonEntity>, List<PersonEntity>> {
    val recordMatches = matchService.findHighConfidenceMatches(personEntities, PersonSearchCriteria.from(personEntity))
    val matchedRecords = recordMatches.map { it.candidateRecord }
    val unmatchedRecords = personEntities.filter { entity -> recordMatches.map { it.candidateRecord.id }.contains(entity.id).not() }
    return Pair(matchedRecords, unmatchedRecords)
  }

  private fun clusterNeedsAttention(personKeyEntity: PersonKeyEntity?) = personKeyEntity?.status == UUIDStatusType.NEEDS_ATTENTION

  private fun clusterHasOneRecord(personKeyEntity: PersonKeyEntity?) = personKeyEntity?.personEntities?.size == 1

  companion object {
    private const val MAX_ATTEMPTS = 5
  }
}
