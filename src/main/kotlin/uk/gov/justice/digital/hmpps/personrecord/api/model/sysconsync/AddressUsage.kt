package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode

data class AddressUsage(
  @Schema(description = "The nomis address usage id", example = "5678")
  val nomisAddressUsageId: Long,
  @Schema(description = "The address usage code", example = "DSH")
  val addressUsageCode: AddressUsageCode,
  @Schema(description = "Is the address active", example = "true")
  val isActive: Boolean,
)
