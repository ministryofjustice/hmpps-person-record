package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "A prisoner phone number")
data class PrisonPhoneNumber(
  @Schema(description = "The CPR contact id", example = "f4165b62-d9eb-406c-bfb1-4f54f4bd2c6d")
  val contactId: String,
  @Schema(description = "Telephone number", example = "0114 2345678")
  val number: String,
  @Schema(description = "Telephone type", example = "TEL")
  val type: String,
  @Schema(description = "Telephone extension number", example = "123")
  val ext: String? = null,
  @Schema(description = "Date phone number was created", example = "2023-01-01T12:00:00")
  val createDateTime: LocalDateTime? = null,
  @Schema(description = "Username of staff member that added phone number")
  val createUserId: String? = null,
  @Schema(description = "Date phone number was last modified", example = "2023-01-01T12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "Username of staff member that last modified phone number")
  val modifyUserId: String? = null,
)
