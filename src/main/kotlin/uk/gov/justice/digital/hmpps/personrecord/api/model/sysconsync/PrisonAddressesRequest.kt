package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class PrisonAddressesRequest(
  @Valid
  @NotEmpty
  val addresses: List<PrisonAddress>,
)
