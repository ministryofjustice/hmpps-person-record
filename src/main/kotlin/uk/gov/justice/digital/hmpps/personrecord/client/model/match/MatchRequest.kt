package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import com.fasterxml.jackson.annotation.JsonProperty

class MatchRequest(
  @JsonProperty("matching_from")
  val matchingFrom: MatchRecord,
  @JsonProperty("matching_to")
  val matchingTo: List<MatchRecord>,
)
