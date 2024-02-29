package uk.gov.justice.digital.hmpps.personrecord.service.matcher

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.model.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.Person

class OffenderMatcher(offenderDetails: List<OffenderDetail>?, person: Person) :
  Matcher<OffenderDetail>(offenderDetails, person) {
  fun isPncDoesNotMatch(): Boolean {
    return !items.isNullOrEmpty() && items.none {
      person.otherIdentifiers?.pncIdentifier == PNCIdentifier.from(it.otherIds.pncNumber)
    }
  }
  override fun isPartialMatchItem(item: OffenderDetail): Boolean {
    return person.otherIdentifiers?.pncIdentifier == PNCIdentifier.from(item.otherIds.pncNumber) &&
      item.firstName.equals(person.givenName, true) || item.surname.equals(person.familyName, true) || item.dateOfBirth == person.dateOfBirth
  }
  override fun isMatchingItem(item: OffenderDetail) =
    person.otherIdentifiers?.pncIdentifier == PNCIdentifier.from(item.otherIds.pncNumber) &&
      item.firstName.equals(person.givenName, ignoreCase = true) &&
      item.surname.equals(person.familyName, ignoreCase = true) &&
      item.dateOfBirth == person.dateOfBirth
}
