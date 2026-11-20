package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconAliasesAndIdentifiersResponseBody(

  val prisonNumber: String,
  val aliasesMappings: List<SysconAliasMapping>,
)

// TODO once sync is on in all environments should make nomisOffenderId and cprAliasId non-nullable
data class SysconAliasMapping(
  val nomisOffenderId: Long? = null,
  val cprAliasId: String? = null,
  val identifierMappings: List<SysconIdentifierMapping> = emptyList(),
)

data class SysconIdentifierMapping(
  val nomisIdentifierId: NomisIdentifierId,
  val cprIdentifierId: String,
)

data class NomisIdentifierId(
  val nomisOffenderId: Long,
  val nomisSequence: Int,
)
