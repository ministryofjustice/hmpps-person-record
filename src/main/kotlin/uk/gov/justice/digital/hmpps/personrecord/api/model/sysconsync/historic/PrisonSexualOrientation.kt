package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class PrisonSexualOrientation(
  @Schema(description = "The sexual orientation code", example = "BIS")
  val sexualOrientationCode: String? = null,
  @Schema(description = "The sexual orientation modify date and time", example = "2000-01-01 12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The sexual orientation modify user id", example = "12345")
  val modifyUserId: String? = null,
  @Schema(description = "The sexual orientation modify display name", example = "John Smith")
  val modifyDisplayName: String? = null,
)
