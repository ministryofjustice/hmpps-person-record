package uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class ProsecutionCase(
  @NotNull
  @Valid
  val defendants: List<Defendant>,
)
