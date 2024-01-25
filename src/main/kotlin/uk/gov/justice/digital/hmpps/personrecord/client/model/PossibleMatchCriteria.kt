package uk.gov.justice.digital.hmpps.personrecord.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PossibleMatchCriteria(
  val firstName: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val pncNumber: String? = null,
  val nomsNumber: String? = null,
) {
  companion object {
    fun from(person: Person): PossibleMatchCriteria =
      PossibleMatchCriteria(
        firstName = person.givenName,
        lastName = person.familyName,
        dateOfBirth = person.dateOfBirth,
        pncNumber = person.otherIdentifiers?.pncIdentifier?.pncId,
      )
  }
}
