package uk.gov.justice.digital.hmpps.personrecord.service

import feign.FeignException
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequest
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequestData
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.MatchScoreClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MATCH_CALL_FAILED

const val MAX_RETRY_ATTEMPTS: Int = 3

@Service
class MatchService(val matchScoreClient: MatchScoreClient, val telemetryService: TelemetryService) {

  private val exceptionsToRetryOn = listOf(HttpClientErrorException::class, HttpServerErrorException::class, FeignException.InternalServerError::class)

  @Value("\${retry.delay}")
  private val retryDelay: Long = 0

  fun score(candidateRecord: PersonEntity, newRecord: Person): MatchResult {
    val candidateRecordIdentifier = candidateRecord.defendantId ?: "defendant1"
    val newRecordIdentifier = newRecord.defendantId ?: "defendant2"

    val matchRequest = MatchRequest(
      uniqueId = MatchRequestData(candidateRecordIdentifier, newRecordIdentifier),
      firstName = MatchRequestData(candidateRecord.firstName, newRecord.givenName),
      surname = MatchRequestData(candidateRecord.lastName, newRecord.familyName),
      dateOfBirth = MatchRequestData(candidateRecord.dateOfBirth.toString(), newRecord.dateOfBirth.toString()),
      pncNumber = MatchRequestData(candidateRecord.pnc?.pncId, newRecord.otherIdentifiers?.pncIdentifier?.pncId),
    )

    val matchScore = getScore(matchRequest)

    return MatchResult(
      matchScore?.matchProbability?.value!!,
      // candidateRecordUUID = candidateRecord.person?.personId.toString(),
      candidateRecordIdentifierType = "defendantId",
      candidateRecordIdentifier = candidateRecordIdentifier,
      newRecordIdentifierType = "defendantId",
      newRecordIdentifier = newRecordIdentifier,
    )
  }

  private fun getScore(matchRequest: MatchRequest): MatchResponse? = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(
        exceptionsToRetryOn,
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
}

class MatchResult(
  val probability: String,
  // val candidateRecordUUID: String,
  val candidateRecordIdentifierType: String,
  val candidateRecordIdentifier: String,
  val newRecordIdentifierType: String,
  val newRecordIdentifier: String,
)
