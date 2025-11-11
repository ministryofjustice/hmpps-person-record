package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class PrisonNationality(
  @Schema(description = "The identifier of the offender source system (NOMIS)", required = true)
  val prisonNumber: String,
  @Schema(description = "The nationality code", example = "BIS")
  val nationalityCode: String,
  @Schema(description = "The nationality start date", example = "1980-01-01")
  val startDate: LocalDate? = null,
  @Schema(description = "The nationality end date", example = "2000-01-01")
  val endDate: LocalDate? = null,
  @Schema(description = "The nationality creation user id", example = "12345")
  val createUserId: String? = null,
  @Schema(description = "The nationality creation date and time", example = "2000-01-01 12:00:00")
  val createDateTime: LocalDateTime? = null,
  @Schema(description = "The nationality creation display name", example = "Other")
  val createDisplayName: String? = null,
  @Schema(description = "The nationality modify date and time", example = "2000-01-01 12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The nationality modify user id", example = "12345")
  val modifyUserId: String? = null,
  @Schema(description = "The nationality modify display name", example = "John Smith")
  val modifyDisplayName: String? = null,
  @Schema(description = "Flag indicating the current nationality", example = "true")
  val current: Boolean,
  @Schema(description = "The nationality notes", example = "Foo bar")
  val notes: String? = null,
)
