package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Create or update a prisoner email address")
data class PrisonEmailAddressRequest(
  @Schema(description = "The email address", example = "foo@bar.example", requiredMode = Schema.RequiredMode.REQUIRED)
  @field:NotBlank
  @field:Size(max = 240)
  val emailAddress: String,

  @Schema(description = "The user who made the change")
  val userId: String,
)
