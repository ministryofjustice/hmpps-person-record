package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

data class Reference(
  val identifierType: IdentifierType,
  val identifierValue: String? = null,
  val identifierRawValue: String? = null,
) {
  companion object {
    fun from(identifierType: IdentifierType, identifierValue: String?, identifierRawValue: String?): Reference = Reference(identifierType = identifierType, identifierValue = identifierValue, identifierRawValue = identifierRawValue)
    fun from(referenceEntity: ReferenceEntity): Reference = Reference(
      identifierType = referenceEntity.identifierType,
      identifierValue = referenceEntity.identifierValue,
    )
  }
}
