package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

data class Contact(
  val contactType: ContactType,
  val contactValue: String? = null,
) {
  companion object {
    fun from(contactType: ContactType, contactValue: String?): Contact? = contactValue.nullIfBlank()?.let {
      Contact(contactType = contactType, contactValue = contactValue.nullIfBlank())
    }

    fun convertEntityToContact(contactEntity: ContactEntity): Contact = Contact(
      contactType = contactEntity.contactType,
      contactValue = contactEntity.contactValue,
    )
  }
}
