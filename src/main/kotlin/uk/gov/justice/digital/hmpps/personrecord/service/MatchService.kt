package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRecord
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequest
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.MatchScoreClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_SCORE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MATCH_CALL_FAILED

const val MAX_RETRY_ATTEMPTS: Int = 3

@Service
class MatchService(
  val matchScoreClient: MatchScoreClient,
  val telemetryService: TelemetryService,
  @Value("\${retry.delay}")
  private val retryDelay: Long = 0,
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

  fun getSelfMatchScore(searchCriteria: PersonSearchCriteria): Pair<Boolean, Double> {
    val matchRecord = MatchRecord.from(searchCriteria)
    val matchRequest = MatchRequest(
      matchingFrom = matchRecord,
      matchingTo = listOf(matchRecord),
    )
    val matchScores = getScores(matchRequest)
    val selfMatchScore = matchScores?.matchProbabilities?.get("0")!!
    return Pair(isAboveThreshold(selfMatchScore), selfMatchScore)
  }

  private fun isAboveThreshold(score: Double): Boolean = score > THRESHOLD_SCORE

  private fun getScores(matchRequest: MatchRequest): MatchResponse? = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(
        MAX_RETRY_ATTEMPTS,
        retryDelay,
      ) { matchScoreClient.getMatchScores(matchRequest) }
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
