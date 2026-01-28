package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Contact as SysconContact

data class Contact(
  val contactType: ContactType,
  val contactValue: String? = null,
  val extension: String? = null,
) {
  companion object {

    fun from(contact: SysconContact): Contact? = contact.value.nullIfBlank()?.let {
      Contact(
        contactType = contact.type,
        contactValue = contact.value,
        extension = contact.extension,
      )
    }

    fun from(contactType: ContactType, contactValue: String?): Contact? = contactValue.nullIfBlank()?.let {
      Contact(contactType = contactType, contactValue = contactValue.nullIfBlank())
    }

    fun from(contactEntity: ContactEntity): Contact = Contact(
      contactType = contactEntity.contactType,
      contactValue = contactEntity.contactValue,
      extension = contactEntity.extension,
    )
  }
}
