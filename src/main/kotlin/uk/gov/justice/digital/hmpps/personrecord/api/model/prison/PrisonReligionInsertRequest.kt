package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode

data class PrisonReligionInsertRequest(
  @Schema(description = "The religion code", example = "ZORO", required = true)
  val religion: ReligionCode,
  @Schema(description = "Reason for the religion change", example = "Some information")
  val comment: String?,
  @Schema(description = "The user id", example = "ABCDEF", required = true)
  val userId: String,
)
