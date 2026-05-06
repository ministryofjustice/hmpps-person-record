package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

data class CanonicalContactType(
  @Schema(description = "Address contact type code", example = "HOME")
  val code: String? = null,
  @Schema(description = "Address contact type description", example = "Home")
  val description: String? = null,
) {
  companion object {

    fun from(contactType: ContactType?) = CanonicalContactType(
      code = contactType?.name,
      description = contactType?.description,
    )
  }
}
