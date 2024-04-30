package uk.gov.justice.digital.hmpps.personrecord.model

import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

class PersonContact(
  val contactType: ContactType,

  val contactValue: String? = null,
) {

  fun isContactValueNullOrEmpty(): Boolean {
    return contactValue.isNullOrEmpty()
  }
}
