package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderMatchCriteria
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.config.ProbationOffenderSearchOAuth2Config

@FeignClient(
  name = "offender-search",
  url = "\${offender-search.base-url}",
  configuration = [ProbationOffenderSearchOAuth2Config::class],
)
interface ProbationOffenderSearchClient {

  @GetMapping(value = ["\${offender-search.offender-detail}"])
  fun findPossibleMatches(@RequestBody offenderMatchCriteria: OffenderMatchCriteria): List<OffenderDetail>?
}
