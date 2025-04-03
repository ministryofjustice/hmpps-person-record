package uk.gov.justice.digital.hmpps.personrecord.service.search

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_SCORE
import java.util.UUID

@Component
class PersonMatchService(
  private val personMatchClient: PersonMatchClient,
  private val retryExecutor: RetryExecutor,
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
) {

  fun findHighestConfidencePersonRecord(personEntity: PersonEntity) = findHighestConfidencePersonRecordsByProbabilityDesc(personEntity).firstOrNull()?.personEntity

  fun findHighestConfidencePersonRecordsByProbabilityDesc(personEntity: PersonEntity): List<PersonMatchResult> = runBlocking {
    val personScores = handleCollectingPersonScores(personEntity)
    val highConfidenceRecords = personScores
      .removeLowQualityMatches()
      .logCandidateScores()
    val highConfidencePersonRecords = getPersonRecords(highConfidenceRecords)
      .allowMatchesWithUUID()
      .removeMatchesWhereClusterHasExcludeMarker(personEntity.id)
      .logCandidateSearchSummary(personEntity, totalNumberOfScores = personScores.size)
      .logHighConfidenceDuplicates()
      .sortedByDescending { it.probability }
    return@runBlocking highConfidencePersonRecords
  }

  fun examineIsClusterValid(cluster: PersonKeyEntity): IsClusterValidResponse = runBlocking {
    checkClusterIsValid(cluster).fold(
      onSuccess = { it },
      onFailure = { throw it },
    )
  }

  private fun getPersonRecords(personScores: List<PersonMatchScore>): List<PersonMatchResult> = personScores.mapNotNull {
    personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))?.let { person ->
      PersonMatchResult(
        probability = it.candidateMatchProbability,
        personEntity = person,
      )
    }
  }

  private fun handleCollectingPersonScores(personEntity: PersonEntity): List<PersonMatchScore> = runBlocking {
    getPersonScores(personEntity).fold(
      onSuccess = { it },
      onFailure = { throw it },
    )
  }

  private fun List<PersonMatchScore>.removeLowQualityMatches(): List<PersonMatchScore> = this.filter { candidate -> isAboveThreshold(candidate.candidateMatchProbability) }

  private fun List<PersonMatchScore>.logCandidateScores(): List<PersonMatchScore> {
    this.forEach { candidate ->
      telemetryService.trackEvent(
        CPR_MATCH_SCORE,
        mapOf(
          EventKeys.PROBABILITY_SCORE to candidate.candidateMatchProbability.toString(),
          EventKeys.MATCH_ID to candidate.candidateMatchId,
        ),
      )
    }
    return this
  }

  private suspend fun getPersonScores(personEntity: PersonEntity): Result<List<PersonMatchScore>> = kotlin.runCatching {
    retryExecutor.runWithRetryHTTP { personMatchClient.getPersonScores(personEntity.matchId.toString()) }
  }

  private fun isAboveThreshold(score: Float): Boolean = score >= THRESHOLD_SCORE

  private fun List<PersonMatchResult>.allowMatchesWithUUID(): List<PersonMatchResult> = this.filter { it.personEntity.personKey != PersonKeyEntity.empty }

  private fun List<PersonMatchResult>.removeMatchesWhereClusterHasExcludeMarker(personRecordId: Long?): List<PersonMatchResult> {
    val clusters: Map<UUID, List<PersonMatchResult>> = this.groupBy { it.personEntity.personKey?.personId!! }
    val excludedClusters: List<UUID> = clusters.filter { (_, records) ->
      records.any { record ->
        record.personEntity.overrideMarkers.any { it.markerType == OverrideMarkerType.EXCLUDE && it.markerValue == personRecordId }
      }
    }.map { it.key }
    return this.filter { candidate -> excludedClusters.contains(candidate.personEntity.personKey?.personId).not() }
  }

  private fun List<PersonMatchResult>.logCandidateSearchSummary(personEntity: PersonEntity, totalNumberOfScores: Int): List<PersonMatchResult> {
    telemetryService.trackPersonEvent(
      CPR_CANDIDATE_RECORD_SEARCH,
      personEntity,
      mapOf(
        EventKeys.RECORD_COUNT to totalNumberOfScores.toString(),
        EventKeys.UUID_COUNT to this.groupBy { match -> match.personEntity.personKey?.personId?.toString() }.size.toString(),
        EventKeys.HIGH_CONFIDENCE_COUNT to this.count().toString(),
        EventKeys.LOW_CONFIDENCE_COUNT to (totalNumberOfScores - this.count()).toString(),
      ),
    )
    return this
  }

  private fun List<PersonMatchResult>.logHighConfidenceDuplicates(): List<PersonMatchResult> {
    this.takeIf { this.size > 1 }?.forEach { candidate ->
      telemetryService.trackPersonEvent(
        CPR_MATCH_PERSON_DUPLICATE,
        personEntity = candidate.personEntity,
        mapOf(
          EventKeys.PROBABILITY_SCORE to candidate.probability.toString(),
          EventKeys.UUID to candidate.personEntity.personKey?.personId?.toString(),
        ),
      )
    }
    return this
  }

  private suspend fun checkClusterIsValid(cluster: PersonKeyEntity): Result<IsClusterValidResponse> = runCatching {
    retryExecutor.runWithRetryHTTP { personMatchClient.isClusterValid(cluster.getRecordsMatchIds()) }
  }

  private fun PersonKeyEntity.getRecordsMatchIds(): List<String> = this.personEntities.map { it.matchId.toString() }

  private companion object {
    const val THRESHOLD_SCORE = 0.999
  }
}

class PersonMatchResult(
  val probability: Float,
  val personEntity: PersonEntity,
)
