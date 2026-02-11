package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema

data class Identifier(
  @Schema(description = "The nomis identifier id", example = "4567")
  val nomisIdentifierId: Long,
  @Schema(description = "The identifier type", example = "PNC")
  val type: IdentifierType,
  @Schema(description = "The identifier value", example = "2000/1234567A")
  val value: String,
  @Schema(description = "The identifier comment", example = "Some comment")
  val comment: String? = null,
)

enum class IdentifierType {
  PNC,
  CRO,
  NINO,
  DL,
}
