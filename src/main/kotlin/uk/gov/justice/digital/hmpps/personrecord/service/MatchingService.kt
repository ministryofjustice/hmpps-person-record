package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person

@Service
class MatchingService {
  fun score(allMatchingItems: List<DefendantEntity>, person: Person): MatchResult {
    return MatchResult(
      "0.999353426",
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
