package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconPersonResponseBody(
  val prisonNumber: String,
  val sysconAddressesAndContactsResponseBody: SysconAddressesAndContactsResponseBody,

)
