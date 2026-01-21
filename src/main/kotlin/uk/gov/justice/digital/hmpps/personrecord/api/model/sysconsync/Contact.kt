package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

data class Contact(
  @Schema(description = "The contact value", example = "01234567890")
  val value: String? = null,
  @Schema(description = "The contact type", example = "HOME")
  val type: ContactType? = null,
  @Schema(description = "The contact extension", example = "235")
  val extension: String? = null,
  @NotNull
  @Schema(description = "Is the contact for a person", example = "false", required = true)
  val isPersonContact: Boolean,
  @NotNull
  @Schema(description = "Is the contact for an address", example = "true", required = true)
  val isAddressContact: Boolean,
)
