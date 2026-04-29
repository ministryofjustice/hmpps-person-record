package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode

data class CanonicalAddressUsageCode(
  @Schema(description = "Address usage code", example = "CURFEW")
  val code: String? = null,
  @Schema(description = "Address usage description", example = "Curfew Order")
  val description: String? = null,
) {
  companion object {

    fun from(addressUsageCode: AddressUsageCode?) = CanonicalAddressUsageCode(
      code = addressUsageCode?.name,
      description = addressUsageCode?.description,
    )
  }
}
