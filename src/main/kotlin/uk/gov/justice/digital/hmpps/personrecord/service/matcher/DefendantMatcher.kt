package uk.gov.justice.digital.hmpps.personrecord.service.matcher

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier

class DefendantMatcher(defendants: List<DefendantEntity>?, person: Person) :
  Matcher<DefendantEntity>(defendants, person) {
  override fun isExactMatch() = items?.any(::isMatchingItem) ?: false
  override fun isPartialMatchItem(item: DefendantEntity): Boolean {
    return person.otherIdentifiers?.pncIdentifier == PNCIdentifier.from(item.pncNumber?.pncId) &&
      item.firstName.equals(person.givenName, true) || item.surname.equals(person.familyName, true) || item.dateOfBirth == person.dateOfBirth
  }

  override fun isMatchingItem(item: DefendantEntity): Boolean {
    return person.otherIdentifiers?.pncIdentifier == PNCIdentifier.from(item.pncNumber?.pncId) &&
      item.firstName.equals(person.givenName, ignoreCase = true) &&
      item.surname.equals(person.familyName, ignoreCase = true) &&
      item.dateOfBirth == person.dateOfBirth
  }

  fun extractMatchingFields(defendant: DefendantEntity): Map<String, String?> =
    mapOf(
      "Surname" to if (defendant.surname.equals(person.familyName)) defendant.surname else null,
      "Forename" to if (defendant.firstName.equals(person.givenName)) defendant.firstName else null,
      "Date of birth" to if (defendant.dateOfBirth?.equals(person.dateOfBirth) == true) defendant.dateOfBirth.toString() else null,
    ).filterValues { it != null }
}
