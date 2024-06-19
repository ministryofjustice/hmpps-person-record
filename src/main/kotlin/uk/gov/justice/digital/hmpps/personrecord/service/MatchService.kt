package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequest
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequestData
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.MatchScoreClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_SCORE_SUMMARY
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MATCH_CALL_FAILED

const val MAX_RETRY_ATTEMPTS: Int = 3

@Service
class MatchService(val matchScoreClient: MatchScoreClient, val telemetryService: TelemetryService) {

  @Value("\${retry.delay}")
  private val retryDelay: Long = 0

  fun findHighConfidenceMatches(candidateRecords: List<PersonEntity>, newRecord: Person): List<MatchResult> {
    val candidateScores: List<MatchResult> = candidateRecords.map { personEntity ->
      score(personEntity, newRecord)
    }
    val highConfidenceMatches = candidateScores.filter { candidate ->
      candidate.probability.toDouble() > THRESHOLD_SCORE
    }
    telemetryService.trackEvent(
      CPR_MATCH_SCORE_SUMMARY,
      mapOf(
        EventKeys.SOURCE_SYSTEM to newRecord.sourceSystemType.name,
        EventKeys.HIGH_CONFIDENCE_TOTAL to highConfidenceMatches.count().toString(),
        EventKeys.LOW_CONFIDENCE_TOTAL to (candidateRecords.size - highConfidenceMatches.count()).toString(),
      ),
    )
    return highConfidenceMatches.sortedByDescending { candidate -> candidate.probability }
  }

  private fun score(candidateRecord: PersonEntity, newRecord: Person): MatchResult {
    val candidateRecordIdentifier = candidateRecord.defendantId ?: "defendant1"
    val newRecordIdentifier = newRecord.defendantId ?: "defendant2"

    val matchRequest = MatchRequest(
      uniqueId = MatchRequestData(candidateRecordIdentifier, newRecordIdentifier),
      firstName = MatchRequestData(candidateRecord.firstName, newRecord.firstName),
      surname = MatchRequestData(candidateRecord.lastName, newRecord.lastName),
      dateOfBirth = MatchRequestData(candidateRecord.dateOfBirth.toString(), newRecord.dateOfBirth.toString()),
      pncNumber = MatchRequestData(candidateRecord.pnc?.pncId, newRecord.otherIdentifiers?.pncIdentifier?.pncId),
    )

    val matchScore = getScore(matchRequest)

    return MatchResult(
      matchScore?.matchProbability?.value!!,
      candidateRecord,
    )
  }

  private fun getScore(matchRequest: MatchRequest): MatchResponse? = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(
        MAX_RETRY_ATTEMPTS,
        retryDelay,
      ) { matchScoreClient.getMatchScore(matchRequest) }
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
  }
}

class MatchResult(
  val probability: String,
  val candidateRecord: PersonEntity,
)
