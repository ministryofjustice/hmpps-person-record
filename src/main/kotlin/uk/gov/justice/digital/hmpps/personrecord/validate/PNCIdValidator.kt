package uk.gov.justice.digital.hmpps.personrecord.validate

class PNCIdValidator() {
  fun isValid(pncIdentifier: PNCIdentifier): Boolean {
    return pncIdentifier.isValid()
  }
}
