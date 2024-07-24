package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

class Reference(
  val identifierType: IdentifierType,
  val identifierValue: String? = null,
) {
  companion object {
    fun from(identifierType: IdentifierType, identifierValue: String?): Reference {
      return Reference(identifierType = identifierType, identifierValue = identifierValue)
    }
  }
}