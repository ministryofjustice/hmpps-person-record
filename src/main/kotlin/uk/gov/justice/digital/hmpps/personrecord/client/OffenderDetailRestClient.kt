package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.personrecord.client.model.DeliusOffenderDetail

@FeignClient(
  name = "offender-detail",
  url = "\${domain-event-and-delius-api.base_url}",
//  configuration = [ProbationOffenderSearchOAuth2Config::class],
)
interface OffenderDetailRestClient {

  @GetMapping("{detailUrl}")
  fun getNewOffenderDetail(@PathVariable("detailUrl") detailUrl: String): DeliusOffenderDetail?
}
