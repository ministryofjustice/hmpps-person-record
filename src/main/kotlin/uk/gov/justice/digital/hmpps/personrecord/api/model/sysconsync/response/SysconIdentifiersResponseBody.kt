package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconIdentifiersResponseBody(
  val prisonNumber: String,
  val identifiersMappings: List<SysconIdentifierMapping>,
)
