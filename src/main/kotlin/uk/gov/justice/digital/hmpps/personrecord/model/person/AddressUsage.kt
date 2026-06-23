package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.SysconAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode

data class AddressUsage(
  val addressUsageCode: AddressUsageCode,
  val isActive: Boolean,
) {

  companion object {

    fun from(usage: SysconAddressUsage): AddressUsage = AddressUsage(usage.addressUsageCode, usage.isActive)

    fun from(usage: ProbationCreateAddressUsage): AddressUsage = AddressUsage(usage.usageCode, usage.isActive)

    fun from(usage: AddressUsageEntity): AddressUsage = AddressUsage(usage.usageCode, usage.active)
  }
}
