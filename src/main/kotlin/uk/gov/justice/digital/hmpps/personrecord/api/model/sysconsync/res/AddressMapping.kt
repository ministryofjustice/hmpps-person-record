package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.res

data class AddressMapping(
  val nomisAddressId: Long,
  val cprAddressId: Long,
  val addressUsageMappings: List<AddressUsageMapping>,
  val addressContactMappings: List<AddressContactMapping>,
)

data class AddressUsageMapping(
  val nomisAddressUsageId: Long,
  val cprAddressUsageid: Long,
)

data class AddressContactMapping(
  val nomisAddressContactId: Long,
  val cprAddressContactId: Long,
)
