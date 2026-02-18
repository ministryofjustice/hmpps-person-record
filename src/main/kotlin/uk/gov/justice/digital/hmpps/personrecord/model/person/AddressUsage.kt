package uk.gov.justice.digital.hmpps.personrecord.model.person

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.AddressUsage as SysconAddressUsage

data class AddressUsage(
  @Schema(description = "The address usage code", example = "DSH")
  val addressUsageCode: AddressUsageCode,
  @Schema(description = "Is the address active", example = "true")
  val isActive: Boolean,
) {

  companion object {

    fun from(usage: SysconAddressUsage): AddressUsage = AddressUsage(usage.addressUsageCode, usage.isActive)

    fun from(usage: AddressUsageEntity): AddressUsage = AddressUsage(usage.usageCode, usage.active)
  }
}
