package uk.gov.justice.digital.hmpps.personrecord.client.model

import uk.gov.justice.digital.hmpps.personrecord.model.Person
import java.time.LocalDate

data class SearchDto(
  val firstName: String? = null,
  val surname: String? = null,
  val dateOfBirth: LocalDate? = null,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val crn: String? = null,
  val nomsNumber: String? = null,
  val includeAliases: Boolean? = false,
) {
  companion object {
    fun from(person: Person): SearchDto {
      return SearchDto(
        firstName = person.givenName,
        surname = person.familyName,
        dateOfBirth = person.dateOfBirth,
        pncNumber = person.otherIdentifiers?.pncNumber,
        croNumber = person.otherIdentifiers?.cro,
        crn = person.otherIdentifiers?.crn,
      )
    }
  }
}
