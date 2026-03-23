package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifiers

data class PrisonAlias(
  val alias: CanonicalAlias? = null,
  val identifiers: CanonicalIdentifiers?,
)
