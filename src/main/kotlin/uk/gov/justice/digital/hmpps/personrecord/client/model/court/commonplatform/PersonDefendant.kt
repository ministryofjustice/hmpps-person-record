package uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class PersonDefendant(
  @Valid
  @NotNull
  val personDetails: PersonDetails? = null,
  val driverNumber: String? = null,
  val arrestSummonsNumber: String? = null,
)
