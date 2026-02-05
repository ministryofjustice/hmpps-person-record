package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

data class Contact(
  @Schema(description = "The nomis person contact id", example = "1234")
  val nomisPersonContactId: Long? = null,
  @Schema(description = "The nomis address contact id", example = "1234")
  val nomisAddressContactId: Long? = null,
  @Schema(description = "The contact value", example = "01234567890")
  val value: String? = null,
  @Schema(description = "The contact type", example = "HOME")
  val type: ContactType,
  @Schema(description = "The contact extension", example = "235")
  val extension: String? = null,
)
