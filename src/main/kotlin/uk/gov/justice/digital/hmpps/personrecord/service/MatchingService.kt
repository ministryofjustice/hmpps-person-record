package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person

@Service
class MatchingService {
  fun score(allMatchingItems: List<DefendantEntity>, person: Person): MatchResult {
    println(allMatchingItems.size)
    return MatchResult("0.999353426", newRecordIdentifierType = "defendantId", newRecordIdentifier = person.defendantId!!)
  }
}

class MatchResult(val probability: String, val newRecordIdentifierType: String, val newRecordIdentifier: String)
