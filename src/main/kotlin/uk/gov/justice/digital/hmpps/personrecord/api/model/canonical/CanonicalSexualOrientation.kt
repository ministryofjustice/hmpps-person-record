package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation

data class CanonicalSexualOrientation(
  @Schema(description = "Person sexual orientation code", example = "HET")
  val code: String? = null,
  @Schema(description = "Person sexual orientation description", example = "Heterosexual")
  val description: String? = null,
) {
  companion object {
    fun from(sexualOrientation: SexualOrientation?) = CanonicalSexualOrientation(
      code = sexualOrientation?.name,
      description = sexualOrientation?.description,
    )
  }
}
