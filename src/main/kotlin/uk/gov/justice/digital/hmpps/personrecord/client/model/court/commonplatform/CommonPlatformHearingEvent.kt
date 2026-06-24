package uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class CommonPlatformHearingEvent(
  @NotNull
  @Valid
  val hearing: Hearing,
)
