package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconAliasesAndIdentifiersResponseBody(

  val prisonNumber: String,
  val aliasesMappings: List<SysconAliasMapping>,
)

data class SysconAliasMapping(
  val nomisOffenderId: Long,
  val cprAliasId: String,
  val identifierMappings: List<SysconIdentifierMapping>,
)

data class SysconIdentifierMapping(
  val nomisIdentifierId: NomisIdentifierId,
  val cprIdentifierId: String,
)

data class NomisIdentifierId(
  val nomisOffenderId: Long,
  val nomisSequence: Int,
)
