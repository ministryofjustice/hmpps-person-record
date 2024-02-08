package uk.gov.justice.digital.hmpps.personrecord.service.matcher

import uk.gov.justice.digital.hmpps.personrecord.client.model.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier

class PrisonerMatcher(prisonerDetails: List<Prisoner>?, person: Person) :
  Matcher<Prisoner>(prisonerDetails, person) {
  fun isPncDoesNotMatch(): Boolean {
    return !items.isNullOrEmpty() && items.none {
      person.otherIdentifiers?.pncIdentifier == PNCIdentifier.from(it.pncNumber)
    }
  }
  override fun isPartialMatchItem(item: Prisoner): Boolean {
    return person.otherIdentifiers?.pncIdentifier == PNCIdentifier.from(item.pncNumber) &&
      item.firstName.equals(person.givenName, true) || item.lastName.equals(person.familyName, true) || item.dateOfBirth == person.dateOfBirth
  }

  override fun isMatchingItem(item: Prisoner): Boolean {
    return person.otherIdentifiers?.pncIdentifier == PNCIdentifier.from(item.pncNumber) &&
      item.firstName.equals(person.givenName, ignoreCase = true) &&
      item.lastName.equals(person.familyName, ignoreCase = true) &&
      item.dateOfBirth == person.dateOfBirth
  }
}
