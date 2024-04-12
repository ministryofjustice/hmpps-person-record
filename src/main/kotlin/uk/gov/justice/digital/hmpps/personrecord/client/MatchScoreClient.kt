package uk.gov.justice.digital.hmpps.personrecord.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(
  name = "match-score",
  url = "\${match-score.base-url}",
)
interface MatchScoreClient {

  @GetMapping("match")
  fun getMatchScore(@RequestBody matchRequest: MatchRequest): MatchResponse?
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MatchResponse(
  @JsonProperty("match_probability")
  val matchProbability: MatchData,
)

data class MatchData(@JsonProperty("0") val value: String)

class MatchRequest
