package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema

data class CanonicalTitle(
  @Schema(description = "Person title code", example = "Mr")
  val code: String? = null,
  @Schema(description = "Person title description", example = "Mr")
  val description: String? = null,

) {
  companion object {

    fun from(title: String?) = CanonicalTitle(
      code = title,
      description = title,
    )
  }
}
