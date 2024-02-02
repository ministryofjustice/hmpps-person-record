package uk.gov.justice.digital.hmpps.personrecord.service.matcher

import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier

class OffenderMatcher(val offenderDetails: List<OffenderDetail>?, val person: Person) {

  fun isExactMatch(): Boolean {
    return offenderDetails?.size == 1 && offenderDetails.any { isMatchingOffender(person, it) }
  }

  fun isPartialMatch(): Boolean {
    return offenderDetails?.size == 1 && offenderDetails.any {
      person.otherIdentifiers?.pncIdentifier == PNCIdentifier(it.otherIds.pncNumber) &&
        it.firstName.equals(person.givenName, true) || it.surname.equals(person.familyName, true) || it.dateOfBirth == person.dateOfBirth
    }
  }

  fun isMultipleMatch(): Boolean {
    return offenderDetails?.size!! > 1 && offenderDetails.all { isMatchingOffender(person, it) }
  }

  fun isPncDoesNotMatch(): Boolean {
    return !offenderDetails.isNullOrEmpty() && offenderDetails.none {
      person.otherIdentifiers?.pncIdentifier == PNCIdentifier(it.otherIds.pncNumber)
    }
  }

  fun getAllMatchingOffenders(): List<OffenderDetail>? {
    return offenderDetails?.filter { isMatchingOffender(person, it) }
  }

  private fun isMatchingOffender(person: Person, offenderDetail: OffenderDetail) =
    person.otherIdentifiers?.pncIdentifier == PNCIdentifier(offenderDetail.otherIds.pncNumber) &&
      offenderDetail.firstName.equals(person.givenName, ignoreCase = true) &&
      offenderDetail.surname.equals(person.familyName, ignoreCase = true) &&
      offenderDetail.dateOfBirth == person.dateOfBirth

  fun getOffenderDetail(): OffenderDetail {
    return offenderDetails!![0]
  }
}
