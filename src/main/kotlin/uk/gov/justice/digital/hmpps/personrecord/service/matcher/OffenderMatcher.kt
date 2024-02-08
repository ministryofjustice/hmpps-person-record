package uk.gov.justice.digital.hmpps.personrecord.service.matcher

import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier


class OffenderMatcher(offenderDetails: List<OffenderDetail>?, person: Person) :
  Matcher<OffenderDetail>(offenderDetails, person) {
  fun isPncDoesNotMatch(): Boolean {
    return !items.isNullOrEmpty() && items.none {
      person.otherIdentifiers?.pncIdentifier == PNCIdentifier.create(it.otherIds.pncNumber)
    }
  }
  override fun isPartialMatchItem(item: OffenderDetail): Boolean {
    return person.otherIdentifiers?.pncIdentifier == PNCIdentifier.create(item.otherIds.pncNumber) &&
      item.firstName.equals(person.givenName, true) || item.surname.equals(person.familyName, true) || item.dateOfBirth == person.dateOfBirth
  }
  override fun isMatchingItem(item: OffenderDetail) =
    person.otherIdentifiers?.pncIdentifier == PNCIdentifier.create(item.otherIds.pncNumber) &&
      item.firstName.equals(person.givenName, ignoreCase = true) &&
      item.surname.equals(person.familyName, ignoreCase = true) &&
      item.dateOfBirth == person.dateOfBirth
}
