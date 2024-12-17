package uk.gov.justice.digital.hmpps.personrecord.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import java.util.UUID

@FeignClient(
  name = "match-score",
  url = "\${match-score.base-url}",
)
interface MatchScoreClient {
  @PostMapping("/person/match")
  fun getMatchScores(@RequestBody matchRequest: MatchRequest): MatchResponse?

  @GetMapping("/health")
  fun getMatchHealth(): MatchStatus?
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MatchResponse(
  @JsonProperty("match_probability")
  val matchProbabilities: MutableMap<String, Double> = mutableMapOf(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MatchStatus(
  val status: String? = null,
)

class MatchRequest(
  @JsonProperty("matching_from")
  val matchingFrom: MatchRecord,
  @JsonProperty("matching_to")
  val matchingTo: List<MatchRecord>,
)

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
        dateOfBirth = searchCriteria.preparedDateOfBirth.value.toString(),
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
