package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.extensions.existsIn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

object ContactBuilder {

  fun buildContacts(person: Person, personEntity: PersonEntity): List<ContactEntity> = person.contacts
    .filterNot { it.contactValue.isNullOrEmpty() }
    .mapNotNull { contact ->
      contact.existsIn(
        childEntities = personEntity.contacts,
        match = { ref, entity -> ref.matches(entity) },
        yes = { it },
        no = { ContactEntity.from(contact) },
      )
    }

  private fun Contact.matches(entity: ContactEntity): Boolean = this.contactType == entity.contactType && this.contactValue == entity.contactValue
}
