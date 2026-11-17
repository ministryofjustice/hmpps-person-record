package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.constraints.NotBlank

@Schema(description = "Update a prisoner reference")
data class PrisonReferenceUpdateRequest(
  @Schema(description = "The value of the reference", example = "1231/XX/121", requiredMode = REQUIRED)
  @field:NotBlank
  val reference: String,

  @Schema(description = "Reference comment", example = "Important info", requiredMode = NOT_REQUIRED, nullable = true)
  val comment: String? = null,

  @Schema(description = "The user who made the change", requiredMode = REQUIRED)
  val userId: String,
)
