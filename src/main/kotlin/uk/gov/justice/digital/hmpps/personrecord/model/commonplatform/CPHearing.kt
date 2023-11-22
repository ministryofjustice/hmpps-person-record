package uk.gov.justice.digital.hmpps.personrecord.model.commonplatform

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

class CPHearing(
  @NotEmpty
  @Valid
  val prosecutionCases: List<CPProsecutionCase>,
)
