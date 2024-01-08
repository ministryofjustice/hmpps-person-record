package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import uk.gov.justice.digital.hmpps.personrecord.client.model.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.config.FeignOAuth2Config
import java.net.URI

@FeignClient(
  name = "offender-detail",
  url = "\${domain_event_and_delius_api.base_url}",
  configuration = [FeignOAuth2Config::class],
)
interface OffenderDetailRestClient {

  @GetMapping()
  fun getNewOffenderDetail(detailUrl: URI): DeliusOffenderDetail?
}
