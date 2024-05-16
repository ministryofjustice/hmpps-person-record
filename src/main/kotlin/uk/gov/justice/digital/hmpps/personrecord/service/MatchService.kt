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
import uk.gov.justice.digital.hmpps.personrecord.model.person.name.Names
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MATCH_CALL_FAILED

const val MAX_RETRY_ATTEMPTS: Int = 3

@Service
class MatchService(val matchScoreClient: MatchScoreClient, val telemetryService: TelemetryService) {

  @Value("\${retry.delay}")
  private val retryDelay: Long = 0

  fun score(candidateRecord: PersonEntity, newRecord: Person): MatchResult {
    val candidateRecordIdentifier = candidateRecord.defendantId ?: "defendant1"
    val newRecordIdentifier = newRecord.defendantId ?: "defendant2"
    val names = Names.from(candidateRecord.names)

    val matchRequest = MatchRequest(
      uniqueId = MatchRequestData(candidateRecordIdentifier, newRecordIdentifier),
      firstName = MatchRequestData(names.preferred?.firstName, newRecord.names.preferred?.firstName),
      surname = MatchRequestData(names.preferred?.lastName, newRecord.names.preferred?.lastName),
      dateOfBirth = MatchRequestData(names.preferred?.dateOfBirth.toString(), newRecord.names.preferred?.dateOfBirth.toString()),
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
