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
  val matchProbability: MatchResponseData,
)

data class MatchResponseData(@JsonProperty("0") val value: String)

class MatchRequest(
  @JsonProperty("unique_id")
  val uniqueId: MatchRequestData,
  @JsonProperty("first_name")
  val firstName: MatchRequestData,
  val surname: MatchRequestData,
  @JsonProperty("dob")
  val dateOfBirth: MatchRequestData,
  @JsonProperty("pnc_number")
  val pncNumber: MatchRequestData,
)

class MatchRequestData(@JsonProperty("0") val value0: String? = "", @JsonProperty("1") val value1: String? = "")
