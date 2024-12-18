package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MatchResponse(
  @JsonProperty("match_probability")
  val matchProbabilities: MutableMap<String, Double> = mutableMapOf(),
)
