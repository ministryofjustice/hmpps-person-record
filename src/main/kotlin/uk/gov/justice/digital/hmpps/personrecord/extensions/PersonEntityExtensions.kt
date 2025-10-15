package uk.gov.justice.digital.hmpps.personrecord.extensions

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC

// Reference
fun List<ReferenceEntity>.getCROs(): List<String> = this.getType(CRO)
fun List<ReferenceEntity>.getPNCs(): List<String> = this.getType(PNC)
fun List<ReferenceEntity>.getType(type: IdentifierType): List<String> = this.filter { it.identifierType == type }.mapNotNull { it.identifierValue }

// Contact
fun List<ContactEntity>.getHome(): ContactEntity? = this.findByType(ContactType.HOME)
fun List<ContactEntity>.getMobile(): ContactEntity? = this.findByType(ContactType.MOBILE)
fun List<ContactEntity>.getEmail(): ContactEntity? = this.findByType(ContactType.MOBILE)
fun List<ContactEntity>.findByType(type: ContactType): ContactEntity? = this.find { it.contactType == type }

// Generic
fun <T, E> T.existsIn(childEntities: List<E>, match: (T, E) -> Boolean, yes: (E) -> E, no: () -> E?): E? {
  val found = childEntities.find { match(this, it) }
  return found?.let { yes(found) } ?: no()
}
