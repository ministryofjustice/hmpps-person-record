package uk.gov.justice.digital.hmpps.personrecord.service.matcher

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier

class OffenderMatcher(offenderDetails: List<OffenderDetail>?, person: Person) :
  Matcher<OffenderDetail>(offenderDetails, person) {
  fun isPncDoesNotMatch(): Boolean {
    return !items.isNullOrEmpty() && items.none {
      person.otherIdentifiers?.pncIdentifier == PNCIdentifier.from(it.otherIds.pncNumber)
    }
  }
  override fun isPartialMatchItem(item: OffenderDetail): Boolean {
    return (item.otherIds.pncNumber.toString().isNotEmpty() && person.otherIdentifiers?.pncIdentifier == PNCIdentifier.from(item.otherIds.pncNumber)) ||
      item.firstName.equals(person.givenName, true) ||
      item.surname.equals(person.familyName, true) ||
      item.dateOfBirth == person.dateOfBirth
  }
  override fun isMatchingItem(item: OffenderDetail) = item.firstName.equals(person.givenName, ignoreCase = true) &&
    item.surname.equals(person.familyName, ignoreCase = true) &&
    item.dateOfBirth == person.dateOfBirth
}
