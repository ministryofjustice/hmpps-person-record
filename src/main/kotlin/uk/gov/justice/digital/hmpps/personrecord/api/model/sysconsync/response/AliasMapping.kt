package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class AliasMapping(
  val nomisPseudonymId: String,
  val cprPseudonymId: String?,
  val identifierMappings: List<IdentifierMapping>,
)

data class IdentifierMapping(
  val nomisIdentifierId: String,
  val cprIdentifierId: String?,
)
