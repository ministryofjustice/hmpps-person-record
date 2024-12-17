package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels

data class PreparedLongStatement(
  val parameterName: String,
  val value: Long?,
)
