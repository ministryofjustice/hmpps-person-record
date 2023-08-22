package uk.gov.justice.digital.hmpps.personrecord.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.ProbationOffenderSearchFeignClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.SearchDto

// TODO delete this!
@RestController
class TestController(
  var client: ProbationOffenderSearchClient,
  var feignClient: ProbationOffenderSearchFeignClient,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping("test")
  fun testing() {
    log.debug("Entered standard weblclient client")
    val offenderDetail = client.getOffenderDetail(SearchDto(surname = "Smith"))
    offenderDetail?.forEach { offender -> println(offender) }
  }

  @GetMapping("feignTest")
  fun testingFeign() {
    log.debug("Entered feign client")
    val offenderDetail = feignClient.getOffenderDetail(SearchDto(surname = "Smith"))
    offenderDetail?.forEach { offender -> println(offender) }
  }
}
