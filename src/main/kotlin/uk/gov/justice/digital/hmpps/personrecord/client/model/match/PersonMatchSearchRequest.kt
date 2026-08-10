package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.PersonSearchRequest
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonMatchSearchRequest(
  val fullName: String,
  val dateOfBirth: LocalDate,
  val firstNameAliases: List<String> = emptyList(),
  val lastNameAliases: List<String> = emptyList(),
  val dateOfBirthAliases: List<LocalDate> = emptyList(),
  val postcodes: List<String> = emptyList(),
) {
  companion object {
    fun from(personSearchRequest: PersonSearchRequest): PersonMatchSearchRequest = PersonMatchSearchRequest(
      fullName = """${personSearchRequest.firstName} ${personSearchRequest.middleName} ${personSearchRequest.lastName}""",
      dateOfBirth = personSearchRequest.dateOfBirth,
      firstNameAliases = personSearchRequest.firstNameAliases,
      lastNameAliases = personSearchRequest.lastNameAliases,
      dateOfBirthAliases = personSearchRequest.dateOfBirthAliases,
      postcodes = personSearchRequest.postcodes,
    )
  }
}
