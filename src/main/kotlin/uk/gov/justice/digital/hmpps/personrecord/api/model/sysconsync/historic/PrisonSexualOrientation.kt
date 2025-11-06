package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PrisonSexualOrientation(
  @Schema(description = "The sexual orientation code", example = "BIS")
  val sexualOrientationCode: String,
  @Schema(description = "The sexual orientation start date", example = "1980-01-01")
  val startDate: LocalDate? = null,
  @Schema(description = "The sexual orientation end date", example = "2000-01-01")
  val endDate: LocalDate? = null,
  @Schema(description = "Flag indicating the current sexual orientation status", example = "true")
  val current: Boolean,
)
