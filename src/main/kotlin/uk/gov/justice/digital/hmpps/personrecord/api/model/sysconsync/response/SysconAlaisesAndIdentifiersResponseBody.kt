package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconAlaisesAndIdentifiersResponseBody(
  val prisonNumber: String,
  val aliasesMappings: List<SysconMapping>,
  val identifiersMappings: List<SysconMapping>,
)

data class SysconMapping(
  val nomisReligionId: String,
  val cprReligionId: String,
)
