package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconAliasWithIdentifiersMapping(
  val nomisOffenderId: Long,
  val cprAliasId: String,
  val identifierMappings: List<SysconIdentifierMapping> = emptyList(),
)
