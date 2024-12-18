package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels

import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference

data class PreparedIdentifierStatement(
  val parameterName: String,
  val reference: Reference,
)
