package uk.gov.justice.digital.hmpps.personrecord.service.search

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.MatchScoreClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_SCORE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MATCH_CALL_FAILED

@Component
class MatchService(
  private val matchScoreClient: MatchScoreClient,
  private val telemetryService: TelemetryService,
  private val retryExecutor: RetryExecutor,
) {

  fun findHighConfidenceMatches(candidateRecords: List<PersonEntity>, searchCriteria: PersonSearchCriteria): List<MatchResult> {
    val highConfidenceMatches = candidateRecords.chunked(MAX_RECORDS).flatMap {
      collectHighConfidenceCandidates(it, searchCriteria)
    }
    return highConfidenceMatches
  }

  private fun collectHighConfidenceCandidates(candidateRecords: List<PersonEntity>, searchCriteria: PersonSearchCriteria): List<MatchResult> {
    val candidateScores: List<MatchResult> = scores(candidateRecords, searchCriteria)
    candidateScores.forEach { candidate ->
      telemetryService.trackEvent(
        CPR_MATCH_SCORE,
        mapOf(
          EventKeys.PROBABILITY_SCORE to candidate.probability.toString(),
          EventKeys.SOURCE_SYSTEM to candidate.candidateRecord.sourceSystem.name,
        ),
      )
    }
    return candidateScores.filter { candidate -> isAboveThreshold(candidate.probability) }
  }

  private fun scores(candidateRecords: List<PersonEntity>, searchCriteria: PersonSearchCriteria): List<MatchResult> {
    val fromMatchRecord = MatchRecord.from(searchCriteria)
    val toMatchRecords: List<MatchingRecord> = candidateRecords.map { personEntity ->
      MatchingRecord(
        matchRecord = MatchRecord.from(personEntity),
        personEntity = personEntity,
      )
    }
    val matchRequest = MatchRequest(
      matchingFrom = fromMatchRecord,
      matchingTo = toMatchRecords.map { it.matchRecord },
    )

    val matchScores = getScores(matchRequest)
    val matchResult: List<MatchResult> = toMatchRecords.mapIndexed { index, matchingRecord ->
      MatchResult(
        probability = matchScores?.matchProbabilities?.get(index.toString())!!,
        candidateRecord = matchingRecord.personEntity,
      )
    }
    return matchResult
  }

  private fun isAboveThreshold(score: Double): Boolean = score > THRESHOLD_SCORE

  private fun getScores(matchRequest: MatchRequest): MatchResponse? = runBlocking {
    try {
      return@runBlocking retryExecutor.runWithRetryHTTP { matchScoreClient.getMatchScores(matchRequest) }
    } catch (exception: Exception) {
      telemetryService.trackEvent(
        MATCH_CALL_FAILED,
        emptyMap(),
      )
      throw exception
    }
  }

  companion object {
    const val THRESHOLD_SCORE = 0.999
    const val MAX_RECORDS = 100
  }
}

class MatchingRecord(
  val matchRecord: MatchRecord,
  val personEntity: PersonEntity,
)

class MatchResult(
  val probability: Double,
  val candidateRecord: PersonEntity,
)
