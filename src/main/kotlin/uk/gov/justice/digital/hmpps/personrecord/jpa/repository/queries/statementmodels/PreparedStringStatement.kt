package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels

data class PreparedStringStatement(
  val parameterName: String,
  val value: String?,
)
