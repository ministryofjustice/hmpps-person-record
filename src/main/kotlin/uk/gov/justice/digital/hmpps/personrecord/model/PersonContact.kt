package uk.gov.justice.digital.hmpps.personrecord.model

import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

class PersonContact(
  val contactType: ContactType,
  val contactValue: String? = null,
) {
  companion object {
    fun from(contactType: ContactType, contactValue: String?): PersonContact {
      return PersonContact(contactType = contactType, contactValue = contactValue)
    }
  }
}
