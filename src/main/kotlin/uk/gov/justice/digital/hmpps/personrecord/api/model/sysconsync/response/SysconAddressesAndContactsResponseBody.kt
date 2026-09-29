package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

data class SysconAddressesAndContactsResponseBody(
  val prisonNumber: String,
  val addressesMappings: List<SysconAddressMapping>,
  val contactMappings: List<SysconContactMapping>,
)

data class SysconAddressMapping(
  val nomisAddressId: Long,
  val cprAddressId: String,
  val addressUsageMappings: List<SysconAddressUsageMapping>,
  val contactMappings: List<SysconContactMapping>,
)

data class SysconAddressUsageMapping(
  val nomisAddressUsageId: Long,
  val nomisAddressUsageCode: AddressUsageCode,
  val cprAddressUsageId: String,
)

data class SysconContactMapping(
  val nomisContactId: Long,
  val nomisContactType: ContactType,
  val cprContactId: String,
)
