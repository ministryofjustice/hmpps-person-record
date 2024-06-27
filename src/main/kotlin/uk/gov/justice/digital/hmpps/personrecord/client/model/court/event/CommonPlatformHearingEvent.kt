package uk.gov.justice.digital.hmpps.personrecord.client.model.court.event

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Hearing

data class CommonPlatformHearingEvent(
  @NotNull
  @Valid
  val hearing: Hearing,
)
