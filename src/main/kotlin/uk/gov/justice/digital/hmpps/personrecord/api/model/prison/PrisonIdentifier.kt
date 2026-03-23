package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

data class PrisonIdentifier(
  val identifierType: IdentifierType,
  val identifierValue: String? = null,
  val comment: String? = null,
)
