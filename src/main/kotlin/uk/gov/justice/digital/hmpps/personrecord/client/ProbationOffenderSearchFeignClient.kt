package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.SearchDto
import uk.gov.justice.digital.hmpps.personrecord.config.FeignOAuth2Config

@FeignClient(
  name = "offender-search",
  url = "\${offender-search.base-url}",
  configuration = [FeignOAuth2Config::class]
 )
interface ProbationOffenderSearchFeignClient {

  @GetMapping(value = ["/search"])
  fun getOffenderDetail(@RequestBody searchDto: SearchDto): List<OffenderDetail>?
}