package uk.gov.justice.digital.hmpps.personrecord.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.SearchDto

// TODO delete this!
@RestController
class TestController(var client: ProbationOffenderSearchClient) {

  @GetMapping("test")
  fun testing() {
    val offenderDetail = client.getOffenderDetail(SearchDto(surname = "Smith"))
    offenderDetail?.forEach { offender -> println(offender) }
  }
}
