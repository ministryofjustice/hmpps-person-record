package uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class PersonDefendant(
  @NotNull
  @Valid
  val personDetails: PersonDetails,
  val driverNumber: String? = null,
  val arrestSummonsNumber: String? = null,
)
