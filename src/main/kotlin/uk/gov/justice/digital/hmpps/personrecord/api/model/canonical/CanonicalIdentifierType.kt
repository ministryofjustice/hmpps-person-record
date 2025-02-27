package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

enum class CanonicalIdentifierType {
  PNC,
  CRO,
  NATIONAL_INSURANCE_NUMBER,
  DRIVER_LICENSE_NUMBER,
  ARREST_SUMMONS_NUMBER,
  CRN,
  PRISON_NUMBER,
  DEFENDANT_ID,
  C_ID, ;

  companion object {
    fun extractIdentifierType(identifierType: IdentifierType): CanonicalIdentifierType = CanonicalIdentifierType.valueOf(identifierType.name)
  }
}
