package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.PrisonerMatchCriteria
import uk.gov.justice.digital.hmpps.personrecord.config.PrisonerClientOAuth2Config

@FeignClient(
  name = "prisoner-search",
  url = "\${prisoner-search.base-url}",
  configuration = [PrisonerClientOAuth2Config::class],
)
interface PrisonerSearchClient {

  @PostMapping(value = ["\${prisoner-search.possible-matches}"])
  fun findPossibleMatches(@RequestBody prisonerMatchCriteria: PrisonerMatchCriteria): List<Prisoner>?
}
