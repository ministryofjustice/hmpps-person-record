package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity

data class CanonicalIdentifier(
  val identifierType: CanonicalIdentifierType,
  val identifierValue: String? = "",
) {
  companion object {
    fun from(referenceEntity: ReferenceEntity): CanonicalIdentifier = CanonicalIdentifier(
      identifierType = CanonicalIdentifierType.extractIdentifierType(referenceEntity.identifierType),
      identifierValue = referenceEntity.identifierValue,
    )

    fun fromReferenceEntityList(referenceEntity: List<ReferenceEntity>): List<CanonicalIdentifier> = referenceEntity.map { from(it) }
  }
}
