package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class PrisonReligion(
  @Schema(description = "The religion code", example = "AGNO")
  val religionCode: String? = null,
  @Schema(description = "The religion change reason known", example = "true")
  val changeReasonKnown: Boolean? = null,
  @Schema(description = "The religion comments", example = "Foo Bar")
  val comments: String? = null,
  @Schema(description = "The religion verified flag", example = "true")
  val verified: Boolean? = null,
  @Schema(description = "The religion start date", example = "1980-01-01")
  val startDate: LocalDate? = null,
  @Schema(description = "The religion end date", example = "2000-01-01")
  val endDate: LocalDate? = null,
  @Schema(description = "The religion modify date and time", example = "2000-01-01 12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The religion modify user id", example = "12345")
  val modifyUserId: String? = null,
  @Schema(description = "Flag indicating the current religion", example = "true", required = true)
  val current: Boolean,
)
