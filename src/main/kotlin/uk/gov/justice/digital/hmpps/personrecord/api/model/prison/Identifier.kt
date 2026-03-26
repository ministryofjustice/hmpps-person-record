package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

data class Identifier(
  @Schema(description = "The identifier type", example = "PNC")
  val type: IdentifierType,
  @Schema(description = "The identifier value", example = "2000/1234567A")
  val value: String? = null,
  @Schema(description = "The identifier comment", example = "Some comment")
  val comment: String? = null,
)
