package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchStatus

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
