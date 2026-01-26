package uk.gov.justice.digital.hmpps.personrecord.jobs

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.time.LocalDateTime

@RestController
class ServiceNowDeliusMergeRequest(private val personRepository: PersonRepository) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/jobs/service-now/generate-delius-merge-requests"])
  fun collectAndReport(): String {
    generate()
    return OK
  }

  fun generate() {
    // find cluster updates yesterday with more than 1 Delius record (not working at the moment)
    personRepository.findByLastModifiedBetween(
      LocalDateTime.now().minusDays(1),
      LocalDateTime.now().minusDays(1).plusHours(1),
    )
      .filter { person ->
        person.personKey?.personEntities?.filter {
          it.sourceSystem == SourceSystemType.DELIUS
        }?.size!! > 1
      }

    // identify 5 clusters with more than 1 Delius record
    // call servicenow
    // write response to db
  }

  companion object {
    private const val OK = "OK"
  }
}
