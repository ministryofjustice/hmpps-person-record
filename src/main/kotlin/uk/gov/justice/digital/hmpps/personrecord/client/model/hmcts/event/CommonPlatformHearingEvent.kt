package uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.event

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform.Hearing

data class CommonPlatformHearingEvent(
  @NotNull
  @Valid
  val hearing: Hearing,
)
