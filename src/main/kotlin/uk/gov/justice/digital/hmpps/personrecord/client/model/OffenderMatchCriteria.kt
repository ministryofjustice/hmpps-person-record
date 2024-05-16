package uk.gov.justice.digital.hmpps.personrecord.client.model

import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import java.time.LocalDate

data class OffenderMatchCriteria(
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
    fun from(person: Person): OffenderMatchCriteria {
      return OffenderMatchCriteria(
        firstName = person.names.preferred?.firstName,
        surname = person.names.preferred?.lastName,
        dateOfBirth = person.names.preferred?.dateOfBirth,
        pncNumber = person.otherIdentifiers?.pncIdentifier?.pncId,
        croNumber = person.otherIdentifiers?.croIdentifier?.croId,
        crn = person.otherIdentifiers?.crn,
      )
    }
  }
}
