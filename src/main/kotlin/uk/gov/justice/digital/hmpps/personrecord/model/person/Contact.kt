package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddressContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import java.time.LocalDateTime

data class Contact(
  val contactType: ContactType,
  val contactValue: String? = null,
  val extension: String? = null,
  val createDateTime: LocalDateTime? = null,
  val createUserId: String? = null,
  val modifyDateTime: LocalDateTime? = null,
  val modifyUserId: String? = null,
) {
  companion object {

    fun from(contact: PrisonContact): Contact? = contact.value.nullIfBlank()?.let {
      Contact(
        contactType = contact.type,
        contactValue = contact.value,
        extension = contact.extension,
        createDateTime = contact.createDateTime,
        createUserId = contact.createUserId,
        modifyDateTime = contact.modifyDateTime,
        modifyUserId = contact.modifyUserId,
      )
    }

    fun from(contact: ProbationCreateAddressContact): Contact? = contact.value.nullIfBlank()?.let {
      Contact(
        contactType = contact.typeCode,
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
      createDateTime = contactEntity.createDateTime,
      createUserId = contactEntity.createUserId,
      modifyDateTime = contactEntity.modifyDateTime,
      modifyUserId = contactEntity.modifyUserId,
    )
  }
}
