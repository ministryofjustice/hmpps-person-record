package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class PrisonSexualOrientation(
  @Schema(description = "The sexual orientation code", example = "BIS")
  val sexualOrientationCode: String? = null,
  @Schema(description = "The sexual orientation modify date and time", example = "2000-01-01 12:00:00", required = true)
  @NotBlank
  var modifyDateTime: LocalDateTime,
  @Schema(description = "The sexual orientation modify user id", example = "12345", required = true)
  @NotBlank
  var modifyUserId: String,
)
