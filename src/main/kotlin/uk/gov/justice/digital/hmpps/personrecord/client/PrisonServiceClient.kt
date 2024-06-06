package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.cloud.openfeign.SpringQueryMap
import org.springframework.web.bind.annotation.GetMapping
import uk.gov.justice.digital.hmpps.personrecord.client.model.PrisonerNumbers
import uk.gov.justice.digital.hmpps.personrecord.config.PrisonServiceOAuth2Config

@FeignClient(
  name = "prison-service",
  url = "\${prison-service.base-url}",
  configuration = [PrisonServiceOAuth2Config::class],
)
interface PrisonServiceClient {

  @GetMapping(value = ["/api/prisoners/prisoner-numbers"])
  fun getPrisonerNumbers(@SpringQueryMap params: PageParams): PrisonerNumbers?
}
class PageParams(val page: Int, val size: Int)
