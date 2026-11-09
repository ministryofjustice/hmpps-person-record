package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

@Schema(description = "Create or update a prisoner contact")
data class PrisonContactRequest(
  @Schema(description = "Type of contact", example = "BUS", requiredMode = REQUIRED)
  val type: ContactType,

  @Schema(description = "The value for the contact", example = "01234 567 890", requiredMode = REQUIRED)
  val value: String,

  @Schema(description = "The contact extension", example = "123", requiredMode = NOT_REQUIRED, nullable = true)
  val extension: String? = null,

  @Schema(description = "The user who made the change", requiredMode = REQUIRED)
  val userId: String,
)
