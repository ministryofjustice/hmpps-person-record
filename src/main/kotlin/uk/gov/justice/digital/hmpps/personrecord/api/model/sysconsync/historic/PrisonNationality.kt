package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class PrisonNationality(
  @Schema(description = "The nationality code", example = "BIS")
  val nationalityCode: String,
  @Schema(description = "The nationality modify date and time", example = "2000-01-01 12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The nationality modify user id", example = "12345")
  val modifyUserId: String? = null,
  @Schema(description = "The nationality modify display name", example = "John Smith")
  val modifyDisplayName: String? = null,
  @Schema(description = "The nationality notes", example = "Foo bar")
  val notes: String? = null,
)
