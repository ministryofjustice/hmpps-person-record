package uk.gov.justice.digital.hmpps.personrecord.api.model

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class CanonicalRecord(
  val personId: String? = null,

) {
  companion object {
    fun from(personKey: PersonKeyEntity): CanonicalRecord = CanonicalRecord(personId = personKey.personId.toString())
  }
}
