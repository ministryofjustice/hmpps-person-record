package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.cloud.openfeign.SpringQueryMap
import org.springframework.web.bind.annotation.GetMapping
import uk.gov.justice.digital.hmpps.personrecord.client.model.ProbationCases
import uk.gov.justice.digital.hmpps.personrecord.config.CorePersonRecordAndDeliusClientOAuth2Config

@FeignClient(
  name = "core-person-record-and-delius",
  url = "\${core-person-record-and-delius.base-url}",
  configuration = [CorePersonRecordAndDeliusClientOAuth2Config::class],
)
interface CorePersonRecordAndDeliusClient {

  @GetMapping("all-probation-cases")
  fun getProbationCases(@SpringQueryMap params: CorePersonRecordAndDeliusClientPageParams): ProbationCases?
}

class CorePersonRecordAndDeliusClientPageParams(val page: Int, val size: Int) {
  val sort: String = "id,asc"
}
