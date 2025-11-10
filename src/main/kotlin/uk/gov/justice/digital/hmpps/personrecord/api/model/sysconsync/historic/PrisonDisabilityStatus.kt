package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PrisonDisabilityStatus(
  @Schema(description = "The identifier of the offender source system (NOMIS)", required = true)
  val prisonNumber: String,
  @Schema(description = "The disability flag", example = "false", required = true)
  val disability: Boolean,
  @Schema(description = "Flag indicating whether data is current or previous", example = "true", required = true)
  val current: Boolean,
  @Schema(description = "The disability start date", example = "1980-01-01")
  val startDate: LocalDate? = null,
  @Schema(description = "The disability end date", example = "2000-01-01")
  val endDate: LocalDate? = null,
)
