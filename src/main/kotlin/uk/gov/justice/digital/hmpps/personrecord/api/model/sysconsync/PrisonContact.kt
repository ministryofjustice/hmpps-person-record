package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import java.time.LocalDateTime

data class PrisonContact(
  @Schema(description = "The nomis contact id", example = "1234")
  val nomisContactId: Long,
  @Schema(description = "The contact value", example = "01234567890")
  val value: String? = null,
  @Schema(description = "The contact type", example = "HOME")
  val type: ContactType,
  @Schema(description = "The contact extension", example = "235")
  val extension: String? = null,
  @Schema(description = "The contact create date and time", example = "2000-01-01 12:00:00", required = true)
  val createDateTime: LocalDateTime,
  @Schema(description = "The contact create user id", example = "12345", required = true)
  val createUserId: String,
  @Schema(description = "The contact modify date and time", example = "2000-01-01 12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The contact modify user id", example = "12345")
  val modifyUserId: String? = null,
)
