package uk.gov.justice.digital.hmpps.personrecord.service.search

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
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
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MATCH_CALL_FAILED
import java.util.UUID

@Component
class PersonMatchService(
  private val personMatchClient: PersonMatchClient,
  private val retryExecutor: RetryExecutor,
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
) {

  fun findHighestConfidencePersonRecord(personEntity: PersonEntity): PersonEntity? = runBlocking {
    val personScores = handleCollectingPersonScores(personEntity)
      .filterAboveThreshold()
      .logCandidateScores()
    val highConfidencePersonRecords = collectPersonRecordsByMatchId(personScores)
      .filterUUIDExists()
      .filterClustersWithExcludeMarker(personEntity.id)
      .logCandidateSearchSummary(personEntity, totalNumberOfScores = personScores.size)
      .sortedByDescending { it.probability }
      .logHighConfidenceDuplicates()
    return@runBlocking highConfidencePersonRecords.firstOrNull()?.personEntity
  }

  private fun collectPersonRecordsByMatchId(personScores: List<PersonMatchScore>): List<PersonMatchResult> = personScores.map {
    PersonMatchResult(
      probability = it.candidateMatchProbability,
      personEntity = personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))!!,
    )
  }

  private fun handleCollectingPersonScores(personEntity: PersonEntity): List<PersonMatchScore> = runBlocking {
    getPersonScores(personEntity).fold(
      onSuccess = { it },
      onFailure = { exception ->
        telemetryService.trackEvent(
          MATCH_CALL_FAILED,
          mapOf(EventKeys.MATCH_ID to personEntity.matchId.toString()),
        )
        throw exception
      },
    )
  }

  private fun List<PersonMatchScore>.filterAboveThreshold(): List<PersonMatchScore> = this.filter { candidate -> isAboveThreshold(candidate.candidateMatchProbability) }

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

  private fun List<PersonMatchResult>.filterUUIDExists(): List<PersonMatchResult> = this.filter { it.personEntity.personKey != PersonKeyEntity.empty }

  private fun List<PersonMatchResult>.filterClustersWithExcludeMarker(personRecordId: Long?): List<PersonMatchResult> {
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
        EventKeys.SOURCE_SYSTEM to personEntity.sourceSystem.name,
        EventKeys.RECORD_COUNT to totalNumberOfScores.toString(),
        EventKeys.UUID_COUNT to this.groupBy { match -> match.personEntity.personKey?.let { it.personId.toString() } }.size.toString(),
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
          EventKeys.SOURCE_SYSTEM to candidate.personEntity.sourceSystem.name,
          EventKeys.PROBABILITY_SCORE to candidate.probability.toString(),
          EventKeys.UUID to candidate.personEntity.personKey?.let { it.personId.toString() },
        ),
      )
    }
    return this
  }

  private companion object {
    const val THRESHOLD_SCORE = 0.999
  }
}

class PersonMatchResult(
  val probability: Float,
  val personEntity: PersonEntity,
)
