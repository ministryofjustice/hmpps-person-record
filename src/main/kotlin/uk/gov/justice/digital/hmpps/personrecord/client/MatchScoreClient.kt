package uk.gov.justice.digital.hmpps.personrecord.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(
  name = "match-score",
  url = "\${match-score.base-url}",
)
interface MatchScoreClient {
  @PostMapping("match")
  fun getMatchScore(@RequestBody matchRequest: MatchRequest): MatchResponse?
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MatchResponse(
  @JsonProperty("match_probability")
  val matchProbability: MatchData,
)

data class MatchData(@JsonProperty("0") val value: String)

class MatchRequest(
  @JsonProperty("unique_id")
  val uniqueId: PersonMatchScoreParameter,
  @JsonProperty("first_name")
  val firstName: PersonMatchScoreParameter,
  val surname: PersonMatchScoreParameter,
  @JsonProperty("dob")
  val dateOfBirth: PersonMatchScoreParameter,
  @JsonProperty("pnc_number")
  val pncNumber: PersonMatchScoreParameter,
  @JsonProperty("source_dataset")
  val sourceDataSet: PersonMatchScoreParameter = PersonMatchScoreParameter("libra", "delius"),
)

class PersonMatchScoreParameter(@JsonProperty("0") val value0: String? = "", @JsonProperty("1") val value1: String? = "")
