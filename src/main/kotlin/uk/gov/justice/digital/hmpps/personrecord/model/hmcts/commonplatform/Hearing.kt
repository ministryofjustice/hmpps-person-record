package uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

class Hearing(
  @NotEmpty
  @Valid
  val prosecutionCases: List<ProsecutionCase>,
)
