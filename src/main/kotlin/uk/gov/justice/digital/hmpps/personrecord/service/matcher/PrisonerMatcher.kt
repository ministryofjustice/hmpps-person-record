package uk.gov.justice.digital.hmpps.personrecord.service.matcher

import uk.gov.justice.digital.hmpps.personrecord.client.model.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier

class PrisonerMatcher(val prisonerDetails: List<Prisoner>?, val person: Person) {

  fun isExactMatch(): Boolean {
    return !prisonerDetails.isNullOrEmpty() && prisonerDetails.size == 1 && prisonerDetails.any { isMatchingPrisoner(person, it) }
  }

  fun isPartialMatch(): Boolean {
    return !prisonerDetails.isNullOrEmpty() && prisonerDetails.size == 1 && prisonerDetails.any {
      person.otherIdentifiers?.pncIdentifier == PNCIdentifier.create(it.pncNumber) &&
        it.firstName.equals(person.givenName, true) || it.lastName.equals(person.familyName, true) || it.dateOfBirth == person.dateOfBirth
    }
  }

  fun isMultipleMatch(): Boolean {
    return !prisonerDetails.isNullOrEmpty() && prisonerDetails.size > 1 && prisonerDetails.all { isMatchingPrisoner(person, it) }
  }

  fun isPncDoesNotMatch(): Boolean {
    return !prisonerDetails.isNullOrEmpty() && prisonerDetails.none {
      person.otherIdentifiers?.pncIdentifier == PNCIdentifier.create(it.pncNumber)
    }
  }

  fun getAllMatchingPrisoners(): List<Prisoner>? {
    return prisonerDetails?.filter { isMatchingPrisoner(person, it) }
  }

  private fun isMatchingPrisoner(person: Person, prisoner: Prisoner) =
    person.otherIdentifiers?.pncIdentifier == PNCIdentifier.create(prisoner.pncNumber) &&
      prisoner.firstName.equals(person.givenName, ignoreCase = true) &&
      prisoner.lastName.equals(person.familyName, ignoreCase = true) &&
      prisoner.dateOfBirth == person.dateOfBirth

  fun getPrisonerDetail(): Prisoner {
    return prisonerDetails!![0]
  }
}
