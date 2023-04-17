package uk.gov.justice.digital.hmpps.personrecord.model

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate

data class PersonDTO(
  val givenName: String? = null,
  val middleNames: List<String>? = emptyList(),
  val familyName: String? = null,
  val dateOfBirth: LocalDate,
  val otherIdentifiers: OtherIdentifiers? = null,
) {
  companion object {

    fun from(personEntity: PersonEntity): PersonDTO {
      return PersonDTO(
        givenName = personEntity.givenName,
        middleNames = personEntity.middleNames?.trim()?.split(" ").orEmpty(),
        familyName = personEntity.familyName,
        dateOfBirth = personEntity.dateOfBirth,
        otherIdentifiers = OtherIdentifiers(
          crn = personEntity.crn,
          pncNumber = personEntity.pncNumber,
        ),
      )
    }
  }
}

data class OtherIdentifiers(
  val crn: String? = null,
  val pncNumber: String? = null,
)
