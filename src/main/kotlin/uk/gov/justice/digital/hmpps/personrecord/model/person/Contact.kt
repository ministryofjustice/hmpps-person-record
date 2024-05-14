package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

class Contact(
  val contactType: ContactType,
  val contactValue: String? = null,
) {
  companion object {
    fun from(contactType: ContactType, contactValue: String?): Contact {
      return Contact(contactType = contactType, contactValue = contactValue)
    }
  }
}
