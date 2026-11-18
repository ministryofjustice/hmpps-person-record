package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

@Schema(description = "Create a prisoner identifier")
data class PrisonIdentifierCreateRequest(
  @Schema(description = "Type of identifier", example = "PNC", requiredMode = REQUIRED)
  val identifierType: IdentifierType,

  @Schema(description = "The value of the identifier", example = "1231/XX/121", requiredMode = REQUIRED)
  @field:NotBlank
  val identifier: String,

  @Schema(description = "Identifier comment", example = "Important info", requiredMode = NOT_REQUIRED, nullable = true)
  val comment: String? = null,

  @Schema(description = "The user who made the change", requiredMode = REQUIRED)
  val userId: String,
)
