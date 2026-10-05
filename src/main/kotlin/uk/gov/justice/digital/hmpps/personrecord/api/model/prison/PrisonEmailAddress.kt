package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "A prisoner email address")
data class PrisonEmailAddress(
  @Schema(description = "The CPR contact id", example = "f4165b62-d9eb-406c-bfb1-4f54f4bd2c6d")
  val contactId: String,
  @Schema(description = "The email address", example = "foo@bar.example")
  val email: String,
  @Schema(description = "Date email address was created", example = "2023-01-01T12:00:00")
  val createDateTime: LocalDateTime? = null,
  @Schema(description = "Username of staff member that added email address")
  val createUserId: String? = null,
  @Schema(description = "Date email address was last modified", example = "2023-01-01T12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "Username of staff member that last modified email address")
  val modifyUserId: String? = null,
)
