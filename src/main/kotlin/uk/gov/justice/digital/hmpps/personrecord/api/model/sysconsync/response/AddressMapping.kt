package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class AddressMapping(
  val nomisAddressId: String,
  val cprAddressId: String?,
  val addressUsageMappings: List<AddressUsageMapping>,
  val addressContactMappings: List<AddressContactMapping>,
)

data class AddressUsageMapping(
  val nomisAddressUsageId: String,
  val cprAddressUsageid: String?,
)

data class AddressContactMapping(
  val nomisContactId: String,
  val cprContactId: String?,
)
