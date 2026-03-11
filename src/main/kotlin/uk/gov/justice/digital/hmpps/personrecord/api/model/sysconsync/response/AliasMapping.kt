package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class AliasMapping(
  val nomisPseudonymId: Long,
  val cprPseudonymId: Long?,
  val identifierMappings: List<IdentifierMapping>,
)

data class IdentifierMapping(
  val nomisIdentifierId: Long,
  val cprIdentifierId: Long,
)
