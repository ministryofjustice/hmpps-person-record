package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class PrisonImmigrationStatus(
  @Schema(description = "The identifier of the offender source system (NOMIS)", required = true)
  val prisonNumber: String,
  @Schema(description = "Flag indicating whether data is an interest to immigration", example = "true", required = true)
  val interestToImmigration: Boolean,
  @Schema(description = "Flag indicating whether data is current or previous", example = "true", required = true)
  val current: Boolean,
  @Schema(description = "The immigration status start date", example = "1980-01-01")
  val startDate: LocalDate? = null,
  @Schema(description = "The immigration status end date", example = "2000-01-01")
  val endDate: LocalDate? = null,
  @Schema(description = "The immigration status creation user id", example = "12345")
  val createUserId: String? = null,
  @Schema(description = "The immigration status creation date and time", example = "2000-01-01 12:00:00")
  val createDateTime: LocalDateTime? = null,
  @Schema(description = "The immigration status creation display name", example = "Other")
  val createDisplayName: String? = null,
  @Schema(description = "The immigration status modify date and time", example = "2000-01-01 12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The immigration status modify user id", example = "12345")
  val modifyUserId: String? = null,
  @Schema(description = "The immigration status modify display name", example = "John Smith")
  val modifyDisplayName: String? = null,
)
