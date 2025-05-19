package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema

data class CanonicalReligion(
  @Schema(description = "Person religion code", example = "Christianity")
  val code: String? = null,
  @Schema(description = "Person religion description", example = "Christianity")
  val description: String? = null,

) {
  companion object {

    fun from(religion: String?) = CanonicalReligion(
      code = religion,
      description = religion,
    )
  }
}
