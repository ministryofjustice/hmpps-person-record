package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonAddressesAndContactsRequest(
  @Schema(description = "List of addresses")
  val addresses: List<PrisonAddress>?,

  @Schema(description = "List of address contacts")
  val contacts: List<PrisonContact>?,
)
