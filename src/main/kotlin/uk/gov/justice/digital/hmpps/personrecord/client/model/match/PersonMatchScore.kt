package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonMatchScore(
  @JsonProperty("candidate_match_id")
  val candidateMatchId: String,
  @JsonProperty("candidate_match_probability")
  val candidateMatchProbability: Float,
  @JsonProperty("candidate_match_weight")
  val candidateMatchWeight: Float,
  @JsonProperty("candidate_should_join")
  val candidateShouldJoin: Boolean,
  @JsonProperty("candidate_should_fracture")
  val candidateShouldFracture: Boolean,
)
