package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconUpdatePersonResponse(
  val prisonerId: String?,
  val addressMappings: List<AddressMapping>,
  val personContactMappings: List<PersonContactMapping>,
  val pseudonymMappings: List<AliasMapping>,
)
