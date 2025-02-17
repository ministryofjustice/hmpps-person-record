package uk.gov.justice.digital.hmpps.personrecord.api.model

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class CanonicalRecord(
  val personId: String? = "",
  val firstName: String? = "",
  val middleNames: String? = "",
  val lastName: String? = "",

) {
  companion object {
    fun from(personKey: PersonKeyEntity): CanonicalRecord {
      val latestPerson = personKey.personEntities.sortedByDescending { it.lastModified }.first()
      return CanonicalRecord(
        personId = personKey.personId.toString(),
        firstName = latestPerson.firstName,
        middleNames = latestPerson.middleNames,
        lastName = latestPerson.lastName,
      )
    }
  }
}
