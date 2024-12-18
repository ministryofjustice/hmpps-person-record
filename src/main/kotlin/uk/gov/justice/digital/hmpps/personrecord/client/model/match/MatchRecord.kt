package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import java.util.*

data class MatchRecord(
  @JsonProperty("unique_id")
  val uniqueId: String? = UUID.randomUUID().toString(),
  @JsonProperty("firstname1")
  val firstName: String? = "",
  val lastname: String? = "",
  @JsonProperty("dob")
  val dateOfBirth: String? = "",
  val pnc: String? = "",
) {
  companion object {

    fun from(searchCriteria: PersonSearchCriteria): MatchRecord {
      return MatchRecord(
        firstName = searchCriteria.preparedFirstName.value,
        lastname = searchCriteria.preparedLastName.value,
        dateOfBirth = searchCriteria.preparedDateOfBirth.value?.toString(),
        pnc = searchCriteria.preparedIdentifiers.firstOrNull { it.reference.identifierType == IdentifierType.PNC }?.reference?.identifierValue,
      )
    }

    fun from(personEntity: PersonEntity): MatchRecord {
      return MatchRecord(
        firstName = personEntity.firstName,
        lastname = personEntity.firstName,
        dateOfBirth = personEntity.dateOfBirth?.toString(),
        pnc = personEntity.references.firstOrNull { it.identifierType == IdentifierType.PNC }?.identifierValue,
      )
    }
  }
}