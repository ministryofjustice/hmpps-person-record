package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Create or update a prisoner phone number")
data class PrisonPhoneNumberRequest(
  @Schema(description = "Type of phone number", example = "BUS", requiredMode = Schema.RequiredMode.REQUIRED)
  @field:NotBlank
  @field:Size(max = 12)
  val phoneNumberType: String,

  @Schema(description = "The phone number", example = "01234 567 890", requiredMode = Schema.RequiredMode.REQUIRED)
  @field:NotBlank
  @field:Size(max = 40)
  val phoneNumber: String,

  @Schema(description = "The telephone extension", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @field:Size(max = 7)
  val extension: String? = null,

  @Schema(description = "The user who made the change")
  val userId: String,
)
