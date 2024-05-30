package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier

data class OtherIdentifiers(
  val crn: String? = null,
  val pncIdentifier: PNCIdentifier? = null,
  val croIdentifier: CROIdentifier? = null,
  var prisonNumber: String? = null,
)
