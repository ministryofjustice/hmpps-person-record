package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

data class CanonicalReference(
  val identifierType: IdentifierType,
  val identifierValue: String? = "",
) {
  companion object {
    fun from(referenceEntity: ReferenceEntity): CanonicalReference = CanonicalReference(
      identifierType = referenceEntity.identifierType,
      identifierValue = referenceEntity.identifierValue,
    )

    fun fromReferenceEntityList(referenceEntity: List<ReferenceEntity>): List<CanonicalReference> = referenceEntity.map { from(it) }
  }
}
