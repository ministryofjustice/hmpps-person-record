package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.res

data class AliasMapping(
  val nomisAliasId: Long,
  val cprAliasId: Long,
  val identifierMappings: List<IdentifierMapping>,
)

data class IdentifierMapping(
  val nomisIdentifierId: Long,
  val cprIdentifierId: Long,
)
