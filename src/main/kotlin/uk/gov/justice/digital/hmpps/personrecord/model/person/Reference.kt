package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

data class Reference(
  val identifierType: IdentifierType,
  val identifierValue: String? = null,
) {
  companion object {
    fun from(referenceEntity: ReferenceEntity): Reference = Reference(
      identifierType = referenceEntity.identifierType,
      identifierValue = referenceEntity.identifierValue,
    )
  }
}
