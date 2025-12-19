package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class PrisonDisabilityStatus(
  @Schema(description = "The disability flag", example = "false", required = true)
  val disability: Boolean,
  @Schema(description = "The disability modify date and time", example = "2000-01-01 12:00:00", required = true)
  val modifyDateTime: LocalDateTime,
  @Schema(description = "The disability modify user id", example = "12345", required = true)
  val modifyUserId: String,
)
