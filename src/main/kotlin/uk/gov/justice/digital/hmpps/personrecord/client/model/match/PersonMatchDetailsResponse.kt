package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import com.fasterxml.jackson.annotation.JsonProperty

data class PersonMatchDetailsResponse(
  @JsonProperty("match_status")
  val matchStatus: MatchStatus,
)

enum class MatchStatus {
  MATCH,
  NO_MATCH,
  POSSIBLE_MATCH,
}
