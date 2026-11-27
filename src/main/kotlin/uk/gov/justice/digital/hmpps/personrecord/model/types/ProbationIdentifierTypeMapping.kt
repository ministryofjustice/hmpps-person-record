package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DL
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NOMS
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PASS

val PROBATION_IDENTIFIER_TYPE_MAPPING: Map<String, IdentifierType> = IdentifierType.entries.associateBy { it.name }
  .plus("PST" to PASS)
  .plus("AI02" to NOMS)
  .plus("DRL" to DL)
