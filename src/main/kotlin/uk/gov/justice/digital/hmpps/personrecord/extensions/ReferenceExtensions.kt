package uk.gov.justice.digital.hmpps.personrecord.extensions

import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

fun List<Reference>.getType(type: IdentifierType): List<Reference> {
  return this.filter { it.identifierType == type }
}

fun List<Reference>.toString(): String {
  return this.joinToString { it.identifierValue.toString() }
}
