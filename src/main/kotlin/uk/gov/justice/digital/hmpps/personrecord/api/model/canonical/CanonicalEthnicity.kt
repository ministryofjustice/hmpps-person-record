package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema

data class CanonicalEthnicity(
  @Schema(description = "Person ethnicity code", example = "British")
  val code: String? = null,
  @Schema(description = "Person ethnicity description", example = "British")
  val description: String? = null,

) {
  companion object {

    fun from(ethnicity: String?) = CanonicalEthnicity(
      code = ethnicity,
      description = ethnicity,
    )
  }
}
