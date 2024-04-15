package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequest
import uk.gov.justice.digital.hmpps.personrecord.client.MatchScoreClient
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchScoreParameter
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person

@Service
class MatchService(val matchScoreClient: MatchScoreClient) {
  fun score(allMatchingItems: List<DefendantEntity>, newRecord: Person): MatchResult {
    val candidateRecord = allMatchingItems[0]
    // TODO a lot of tidying up
    val candidateRecordIdentifier = candidateRecord.defendantId!!
    val newRecordIdentifier = newRecord.defendantId!!

    val matchScore = matchScoreClient.getMatchScore(
      MatchRequest(
        uniqueId = PersonMatchScoreParameter(candidateRecordIdentifier, newRecordIdentifier),
        firstName = PersonMatchScoreParameter(candidateRecord.firstName, newRecord.givenName),
        surname = PersonMatchScoreParameter(candidateRecord.surname, newRecord.familyName),
        dateOfBirth = PersonMatchScoreParameter(candidateRecord.dateOfBirth.toString(), newRecord.dateOfBirth.toString()),
        pncNumber = PersonMatchScoreParameter(candidateRecord.pncNumber?.pncId, newRecord.otherIdentifiers?.pncIdentifier?.pncId),
      ),
    )

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
