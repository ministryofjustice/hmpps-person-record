package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode

data class CanonicalEthnicity(
  @Schema(description = "Person ethnicity code", example = "B9")
  val code: String? = null,
  @Schema(description = "Person ethnicity description", example = "Black/Black British : Any other backgr'nd")
  val description: String? = null,

) {
  companion object {

    fun from(ethnicity: EthnicityCode?) = CanonicalEthnicity(
      code = ethnicity?.name,
      description = ethnicity?.description,
    )
  }
}
