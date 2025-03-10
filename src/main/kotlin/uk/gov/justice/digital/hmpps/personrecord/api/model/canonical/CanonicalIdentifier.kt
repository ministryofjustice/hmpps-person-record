package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity

@Schema(description = "Possible identifier types")
data class CanonicalIdentifier(
  @Schema(description = "Person Identifier type", example = "CRN")
  val identifierType: CanonicalIdentifierType,
  @Schema(description = "Person Identifier value", example = "B123456")
  val identifierValue: List<String?> = emptyList(),
) {
  companion object {
    private fun from(referenceEntity: ReferenceEntity): CanonicalIdentifier = CanonicalIdentifier(
      identifierType = CanonicalIdentifierType.extractIdentifierType(referenceEntity.identifierType),
      identifierValue = listOf(referenceEntity.identifierValue),
    )

    fun fromReferenceEntityList(referenceEntity: List<ReferenceEntity>): List<CanonicalIdentifier> = referenceEntity.map { from(it) }
  }
}
