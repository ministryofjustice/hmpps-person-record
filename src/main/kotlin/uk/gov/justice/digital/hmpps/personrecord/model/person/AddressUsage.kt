package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import java.time.LocalDateTime

data class AddressUsage(
  val addressUsageCode: AddressUsageCode,
  val isActive: Boolean,
  val createDateTime: LocalDateTime? = null,
  val createUserId: String? = null,
  val modifyDateTime: LocalDateTime? = null,
  val modifyUserId: String? = null,
) {

  companion object {

    fun from(usage: PrisonAddressUsage): AddressUsage = AddressUsage(
      usage.addressUsageCode,
      usage.isActive,
      usage.createDateTime,
      usage.createUserId,
      usage.modifyDateTime,
      usage.modifyUserId,
    )

    fun from(usage: ProbationCreateAddressUsage): AddressUsage = AddressUsage(usage.usageCode, usage.isActive)

    fun from(usage: AddressUsageEntity): AddressUsage = AddressUsage(
      usage.usageCode,
      usage.active,
      usage.createDateTime,
      usage.createUserId,
      usage.modifyDateTime,
      usage.modifyUserId,
    )
  }
}
