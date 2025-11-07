package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class PrisonSexualOrientation(
  @Schema(description = "The identifier of the offender source system (NOMIS)", required = true)
  val prisonNumber: String,
  @Schema(description = "The sexual orientation code", example = "BIS")
  val sexualOrientationCode: String,
  @Schema(description = "The sexual orientation start date", example = "1980-01-01")
  val startDate: LocalDate? = null,
  @Schema(description = "The sexual orientation end date", example = "2000-01-01")
  val endDate: LocalDate? = null,
  @Schema(description = "The sexual orientation creation date and time", example = "2000-01-01 12:00:00")
  val createDateTime: LocalDateTime? = null,
  @Schema(description = "The sexual orientation creation display name", example = "Other")
  val createDisplayName: String? = null,
  @Schema(description = "The sexual orientation modify date and time", example = "2000-01-01 12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The sexual orientation modify user id", example = "12345")
  val modifyUserId: String? = null,
  @Schema(description = "The sexual orientation modify display name", example = "John Smith")
  val modifyDisplayName: String? = null,
  @Schema(description = "Flag indicating the current sexual orientation status", example = "true")
  val current: Boolean,
)
