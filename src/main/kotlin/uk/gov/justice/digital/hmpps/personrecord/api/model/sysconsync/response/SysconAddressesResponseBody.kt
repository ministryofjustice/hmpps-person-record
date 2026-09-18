package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconAddressesResponseBody(
  val prisonNumber: String,
  val addressesMappings: List<SysconAddressMapping>,
)

data class SysconAddressMapping(
  val nomisAddressId: Long,
  val cprAddressId: String,
)
