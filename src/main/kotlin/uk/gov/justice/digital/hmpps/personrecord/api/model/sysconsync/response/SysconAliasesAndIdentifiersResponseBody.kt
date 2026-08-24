package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconAliasesAndIdentifiersResponseBody(
  val prisonNumber: String,
  val aliasesMappings: List<SysconAliasMapping>,
  val identifiersMappings: List<SysconIdentifierMapping>,
)

data class SysconAliasMapping(
  val nomisAliasId: String,
  val cprReligionId: String,
)

data class SysconIdentifierMapping(
  val nomisIdentifierId: NomisIdentifierId,
  val cprReligionId: String,
)

data class NomisIdentifierId(
  val nomisOffenderId: Long,
  val nomisSequence: Int,
)
