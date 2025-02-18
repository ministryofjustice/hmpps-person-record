package uk.gov.justice.digital.hmpps.personrecord.api.model

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class CanonicalRecord(
  val id: String? = "",
  val firstName: String? = "",
  val middleNames: String? = "",
  val lastName: String? = "",
  val dateOfBirth: String? = "",
  val crn: String? = "",
  var prisonNumber: String? = "",
  var defendantId: String? = "",
  val title: String? = "",
  val masterDefendantId: String? = "",
  val nationality: String? = "",
  val religion: String? = "",
  val ethnicity: String? = "",
  val cid: String? = "",

) {
  companion object {
    fun from(personKey: PersonKeyEntity): CanonicalRecord {
      val latestPerson = personKey.personEntities.sortedByDescending { it.lastModified }.first()
      return CanonicalRecord(
        id = personKey.personId.toString(),
        firstName = latestPerson.firstName,
        middleNames = latestPerson.middleNames,
        cid = latestPerson.cId,
        lastName = latestPerson.lastName,
        dateOfBirth = latestPerson.dateOfBirth?.toString() ?: "",
        crn = latestPerson.crn,
        prisonNumber = latestPerson.prisonNumber,
        defendantId = latestPerson.defendantId,
        title = latestPerson.title,
        masterDefendantId = latestPerson.masterDefendantId,
        nationality = latestPerson.nationality,
        religion = latestPerson.religion,
        ethnicity = latestPerson.ethnicity,

      )
    }
  }
}
