package uk.gov.justice.digital.hmpps.personrecord.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class PersonIdentifierRecord(
  @Schema(description = "The person identifier. For nDelius this is CRN", example = "P819069")
  val id: String,
  @Schema(description = "The source system", example = "NOMIS")
  val sourceSystem: String,
)
