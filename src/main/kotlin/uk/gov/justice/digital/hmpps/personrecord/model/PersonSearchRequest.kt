package uk.gov.justice.digital.hmpps.personrecord.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PersonSearchRequest(
  @Schema(description = "PNC Number", example = "1965/0046583U")
  val pncNumber: String? = null,
  @Schema(description = "CRN", example = "X340906")
  val crn: String? = null,
  @Schema(description = "The person's first name", example = "Bill")
  val firstName: String? = null,
  @Schema(description = "The person's first middle name", example = "Stuart")
  val middleName: String? = null,
  @Schema(description = "The person's surname", example = "Roberts")
  val surname: String? = null,
  @Schema(description = "The person's date of birth", example = "1980-01-21")
  val dateOfBirth: LocalDate? = null,
) {
  companion object {
    fun from(person: Person): PersonSearchRequest {
      return PersonSearchRequest(
        pncNumber = person.otherIdentifiers?.pncIdentifier?.pncId,
        crn = person.otherIdentifiers?.crn,
        firstName = person.givenName,
        surname = person.familyName,
        dateOfBirth = person.dateOfBirth,
      )
    }
  }
}
