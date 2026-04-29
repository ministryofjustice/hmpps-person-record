package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode

data class CanonicalCountry(
  @Schema(description = "Country code", example = "GBR")
  val code: String? = null,
  @Schema(description = "Country description", example = "w")
  val description: String? = null,

) {
  companion object {

    fun from(countryCode: CountryCode?) = CanonicalCountry(
      code = countryCode?.name,
      description = countryCode?.description,
    )
  }
}
