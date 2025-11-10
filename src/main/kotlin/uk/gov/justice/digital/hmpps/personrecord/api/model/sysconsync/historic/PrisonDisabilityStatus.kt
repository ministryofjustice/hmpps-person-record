package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonDisabilityStatus(
  @Schema(description = "The identifier of the offender source system (NOMIS)", required = true)
  val prisonNumber: String,
  @Schema(description = "The disability flag", example = "false", required = true)
  val disability: Boolean,
  @Schema(description = "Flag indicating whether data is current or previous", example = "true", required = true)
  val current: Boolean,
)
