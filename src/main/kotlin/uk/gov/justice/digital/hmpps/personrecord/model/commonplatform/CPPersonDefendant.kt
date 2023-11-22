package uk.gov.justice.digital.hmpps.personrecord.model.commonplatform

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class CPPersonDefendant(
  @NotNull
  @Valid
  val personDetails: CPPersonDetails,
)
