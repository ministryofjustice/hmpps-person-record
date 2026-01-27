package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.time.LocalDateTime

@Profile("!preprod & !prod")
@RestController
class ServiceNowDeliusMergeRequest(private val personRepository: PersonRepository, private val serviceNowClient: ServiceNowClient) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/jobs/service-now/generate-delius-merge-requests"])
  fun collectAndReport(): ServiceNowResponse {
    val recordsToProcess = generate()
    val payload = ServiceNowPayload(
      sysParmId = "",
      quantity = 1,
      variables = Variables(
        requester = "",
        requestedFor = "",
        details = recordsToProcess.map { NDeliusRecord.from(it) },
      ),
    )
    return serviceNowClient.postRecords(payload)
  }

  fun generate(): List<PersonEntity> {
    // TODO: persist processed clusters to db
    // TODO: extract processed clusters from db
    // TODO: exclude processed clusters
    val deliusRecords = personRepository.findByLastModifiedBetween(
      LocalDateTime.now().minusDays(1),
      LocalDateTime.now().minusDays(1).plusHours(1),
    )
      .filter { person ->
        person.personKey?.personEntities?.filter {
          it.sourceSystem == SourceSystemType.DELIUS
        }?.size!! > 1
      }
    val uniquePersonKey = deliusRecords.map { it.personKey?.personUUID }.toSet().take(CLUSTER_TO_PROCESS_COUNT)
    return deliusRecords.filter { it.personKey?.personUUID in uniquePersonKey }
  }

  companion object {
    private const val CLUSTER_TO_PROCESS_COUNT = 5
  }
}
