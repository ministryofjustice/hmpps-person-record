package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.personrecord.client.model.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.config.FeignOAuth2Config

@FeignClient(
  name = "offender-detail",
  url = "\${domain_event_and_delius_api.base_url}",
  configuration = [FeignOAuth2Config::class],
)
interface OffenderDetailRestClient {

  @GetMapping("{detailUrl}")
  fun getNewOffenderDetail(@PathVariable("detailUrl") detailUrl: String): DeliusOffenderDetail?
}
