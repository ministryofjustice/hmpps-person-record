package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Profile("!preprod & !prod")
@RestController
class ServiceNowMergeRequestController(
  private val serviceNowMergeRequestService: ServiceNowMergeRequestService,
) {

  @Hidden
  @RequestMapping(method = [POST], value = ["/jobs/service-now/generate-delius-merge-requests"])
  suspend fun collectAndReport(): String {

    CoroutineScope(Dispatchers.Default).launch {

    serviceNowMergeRequestService.sendMergeRequest()
    }
    return "ok"
  }

}
