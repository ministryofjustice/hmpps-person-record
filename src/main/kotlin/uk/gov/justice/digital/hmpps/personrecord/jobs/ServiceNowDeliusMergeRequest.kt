package uk.gov.justice.digital.hmpps.personrecord.jobs

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceNowDeliusMergeRequest {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/jobs/service-now/generate-delius-merge-requests"])
  fun collectAndReport(): String {
    generate()
    return OK
  }

  fun generate() {
    // call servicenow, write to db etc
  }

  companion object {
    private const val OK = "OK"
  }
}
