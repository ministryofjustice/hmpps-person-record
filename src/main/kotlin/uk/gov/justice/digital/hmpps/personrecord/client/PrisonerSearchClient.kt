package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.config.PrisonerSearchClientOAuth2Config

@FeignClient(
  name = "prisoner-search",
  url = "\${prisoner-search.base-url}",
  configuration = [PrisonerSearchClientOAuth2Config::class],
)
interface PrisonerSearchClient {

  @GetMapping(value = ["/prisoner/{prisonerNumber}"])
  fun getPrisoner(@PathVariable("prisonerNumber") prisonerNumber: String): Prisoner
}
