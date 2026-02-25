package uk.gov.justice.digital.hmpps.personrecord.client.model.match

data class MatchDetailsResponse(
  val matchStatus: MatchStatus,
)

enum class MatchStatus {
  MATCH,
  NO_MATCH,
  POSSIBLE_MATCH,
}
