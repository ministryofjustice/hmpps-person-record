package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels

data class PreparedIntStatement(
  val parameterName: String,
  val value: Int?,
)
