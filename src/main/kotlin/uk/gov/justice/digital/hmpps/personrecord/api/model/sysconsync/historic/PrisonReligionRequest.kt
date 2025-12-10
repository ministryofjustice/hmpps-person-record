package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class PrisonReligionRequest(
  @Valid
  @Size(min = 1)
  @NotNull
  @Schema(description = "The list of religions for a given prison number", required = true)
  val religions: List<PrisonReligion>,
)
