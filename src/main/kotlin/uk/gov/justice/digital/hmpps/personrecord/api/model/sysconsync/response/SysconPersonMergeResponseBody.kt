package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconPersonMergeResponseBody(
  val toPrisonNumber: String,
  val sysconAddressesAndContactsResponseBody: SysconAddressesAndContactsResponseBody,
)
