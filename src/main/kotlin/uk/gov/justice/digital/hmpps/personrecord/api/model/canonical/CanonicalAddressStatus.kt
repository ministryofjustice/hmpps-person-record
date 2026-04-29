package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode

data class CanonicalAddressStatus(
  @Schema(description = "Address status code", example = "M")
  val code: String? = null,
  @Schema(description = "Address status description", example = "Main")
  val description: String? = null,

) {
  companion object {

    fun from(addressStatusCode: AddressStatusCode?) = CanonicalAddressStatus(
      code = addressStatusCode?.name,
      description = addressStatusCode?.description,
    )
  }
}
