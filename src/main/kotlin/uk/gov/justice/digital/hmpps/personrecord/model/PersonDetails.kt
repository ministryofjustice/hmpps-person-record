package uk.gov.justice.digital.hmpps.personrecord.model

import com.fasterxml.jackson.annotation.JsonInclude
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate
import java.util.UUID
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonDetails(
  val personId: UUID? = null,
  val givenName: String? = null,
  val middleNames: List<String>? = emptyList(),
  val familyName: String,
  val dateOfBirth: LocalDate,
  val otherIdentifiers: OtherIdentifiers? = null,
) {
  companion object {

    fun from(personEntity: PersonEntity): PersonDetails {
      return PersonDetails(
        personId = personEntity.personId,
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
