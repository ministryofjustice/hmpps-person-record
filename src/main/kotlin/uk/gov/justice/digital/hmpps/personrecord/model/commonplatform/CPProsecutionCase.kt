package uk.gov.justice.digital.hmpps.personrecord.model.commonplatform

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class CPProsecutionCase(
  @NotNull
  @Valid
  val defendants: List<CPDefendant>,
)
