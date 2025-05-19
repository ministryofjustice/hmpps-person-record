package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode

data class CanonicalSex(
  @Schema(description = "Person sex code", example = "M")
  val code: String? = null,
  @Schema(description = "Person sex description", example = "Male")
  val description: String? = null,
) {
  companion object {
    fun from(sexCode: SexCode?) = CanonicalSex(
      code = sexCode?.name,
      description = sexCode?.description,
    )
  }
}
