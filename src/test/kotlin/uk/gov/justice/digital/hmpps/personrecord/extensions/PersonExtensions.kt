package uk.gov.justice.digital.hmpps.personrecord.extensions

import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

fun List<Contact>.getType(type: ContactType): List<Contact> = this.filter { it.contactType == type }
fun List<Reference>.getType(type: IdentifierType): List<Reference> = this.filter { it.identifierType == type }
