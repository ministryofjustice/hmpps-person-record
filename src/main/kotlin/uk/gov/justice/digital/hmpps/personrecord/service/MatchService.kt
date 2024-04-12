package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequest
import uk.gov.justice.digital.hmpps.personrecord.client.MatchScoreClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person

@Service
class MatchService(val matchScoreClient: MatchScoreClient) {
  fun score(allMatchingItems: List<DefendantEntity>, person: Person): MatchResult {
    val matchScore = matchScoreClient.getMatchScore(matchRequest = MatchRequest())
    return MatchResult(
      matchScore?.matchProbability?.value!!,
      candidateRecordUUID = allMatchingItems[0].person?.personId.toString(),
      candidateRecordIdentifierType = "defendantId",
      candidateRecordIdentifier = allMatchingItems[0].defendantId!!,
      newRecordIdentifierType = "defendantId",
      newRecordIdentifier = person.defendantId!!,
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
