package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRecord
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequest
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.MatchScoreClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MATCH_CALL_FAILED

const val MAX_RETRY_ATTEMPTS: Int = 3

@Service
class MatchService(
  val matchScoreClient: MatchScoreClient,
  val telemetryService: TelemetryService,
  @Value("\${retry.delay}")
  private val retryDelay: Long = 0
) {

  fun findHighConfidenceMatches(candidateRecords: List<PersonEntity>, newRecord: Person): List<MatchResult> {
    val highConfidenceMatches = candidateRecords.chunked(MAX_RECORDS).flatMap {
      collectHighConfidenceCandidates(it, newRecord)
    }
    return highConfidenceMatches
  }

  private fun collectHighConfidenceCandidates(candidateRecords: List<PersonEntity>, newRecord: Person): List<MatchResult> {
    val candidateScores: List<MatchResult> = scores(candidateRecords, newRecord)
    return candidateScores.filter { candidate ->
      candidate.probability > THRESHOLD_SCORE
    }
  }

  private fun scores(candidateRecords: List<PersonEntity>, newRecord: Person): List<MatchResult> {
    val fromMatchRecord = MatchRecord(
      firstName = newRecord.firstName,
      lastname = newRecord.lastName,
      dateOfBirth = newRecord.dateOfBirth?.toString(),
      pnc = newRecord.otherIdentifiers?.let { it.pncIdentifier?.pncId },
    )
    val toMatchRecords: List<MatchingRecord> = candidateRecords.map { personEntity ->
      MatchingRecord(
        matchRecord = MatchRecord(
          firstName = personEntity.firstName,
          lastname = personEntity.firstName,
          dateOfBirth = personEntity.dateOfBirth?.toString(),
          pnc = personEntity.pnc?.pncId,
        ),
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
    const val MAX_RECORDS = 50
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
