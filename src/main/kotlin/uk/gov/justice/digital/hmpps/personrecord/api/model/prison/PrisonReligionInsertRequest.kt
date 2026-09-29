package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode

@Schema(description = "Insert to prisoner religion")
data class PrisonReligionInsertRequest(
  @Schema(description = "The religion code", example = "ZORO")
  val religionCode: ReligionCode,
  @Schema(description = "Reason for the religion change", example = "Some information")
  val comment: String? = null,
  @Schema(description = "The user who made the change")
  val userId: String,
)
