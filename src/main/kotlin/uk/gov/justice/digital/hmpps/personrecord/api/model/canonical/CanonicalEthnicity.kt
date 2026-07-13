package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode

data class CanonicalEthnicity(
  @Schema(
    description = "Person ethnicity code",
    example = "B9",
    allowableValues = ["A1", "A2", "A3", "A4", "A9", "B1", "B2", "B9", "ETH03", "M1", "M2", "M3", "M9", "MERGE", "NS", "O1", "O2", "O9", "UN", "W1", "W2", "W3", "W4", "W5", "W8", "W9", "Z1"],
  )
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
