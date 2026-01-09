package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class PrisonReligionRequest(
  @Valid
  @NotEmpty
  @Schema(description = "The list of religions for a given prison number", required = true)
  val religions: List<PrisonReligion>,
)
