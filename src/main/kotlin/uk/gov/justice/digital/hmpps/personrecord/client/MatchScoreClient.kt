package uk.gov.justice.digital.hmpps.personrecord.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@FeignClient(
  name = "match-score",
  url = "\${match-score.base-url}",
)
interface MatchScoreClient {
  @PostMapping("/person/match")
  fun getMatchScores(@RequestBody matchRequest: MatchRequest): MatchResponse?
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MatchResponse(
  @JsonProperty("match_probability")
  val matchProbabilities: MutableMap<String, Double> = mutableMapOf(),
)

class MatchRequest(
  @JsonProperty("matching_from")
  val matchingFrom: MatchRecord,
  @JsonProperty("matching_to")
  val matchingTo: List<MatchRecord>,
)

class MatchRecord(
  @JsonProperty("unique_id")
  val uniqueId: String? = UUID.randomUUID().toString(),
  @JsonProperty("firstname1")
  val firstName: String? = "",
  val lastname: String? = "",
  @JsonProperty("dob")
  val dateOfBirth: String? = "",
  val pnc: String? = "",
)
