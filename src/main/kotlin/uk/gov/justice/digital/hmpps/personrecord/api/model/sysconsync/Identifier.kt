package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema

data class Identifier(
  @Schema(description = "The identifier type", example = "PNC")
  val type: IdentifierType? = null,
  @Schema(description = "The identifier value", example = "2000/1234567A")
  val value: String? = null,
)

enum class IdentifierType {
  PNC,
  CRO,
  NINO,
  DL,
}
