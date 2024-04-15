package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequest
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequestData
import uk.gov.justice.digital.hmpps.personrecord.client.MatchScoreClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person

@Service
class MatchService(val matchScoreClient: MatchScoreClient) {
  fun score(candidateRecord: DefendantEntity, newRecord: Person): MatchResult {
    val candidateRecordIdentifier = candidateRecord.defendantId ?: ""
    val newRecordIdentifier = newRecord.defendantId!!

    val matchRequest = MatchRequest(
      uniqueId = MatchRequestData(candidateRecordIdentifier, newRecordIdentifier),
      firstName = MatchRequestData(candidateRecord.firstName, newRecord.givenName),
      surname = MatchRequestData(candidateRecord.surname, newRecord.familyName),
      dateOfBirth = MatchRequestData(candidateRecord.dateOfBirth.toString(), newRecord.dateOfBirth.toString()),
      pncNumber = MatchRequestData(candidateRecord.pncNumber?.pncId, newRecord.otherIdentifiers?.pncIdentifier?.pncId),
    )

    val matchScore = matchScoreClient.getMatchScore(matchRequest)

    return MatchResult(
      matchScore?.matchProbability?.value!!,
      candidateRecordUUID = candidateRecord.person?.personId.toString(),
      candidateRecordIdentifierType = "defendantId",
      candidateRecordIdentifier = candidateRecordIdentifier,
      newRecordIdentifierType = "defendantId",
      newRecordIdentifier = newRecordIdentifier,
    )
  }
}

class MatchResult(
  val probability: String,
  val candidateRecordUUID: String,
  val candidateRecordIdentifierType: String,
  val candidateRecordIdentifier: String,
  val newRecordIdentifierType: String,
  val newRecordIdentifier: String,
)
