package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import java.time.LocalDateTime
import java.util.UUID

@Profile("!preprod & !prod")
@RestController
class ServiceNowMergeRequestController(
  private val personRepository: PersonRepository,
  private val serviceNowMergeRequestClient: ServiceNowMergeRequestClient,
  private val serviceNowMergeRequestRepository: ServiceNowMergeRequestRepository,
  @Value($$"${service-now.sysparm-id}")private val sysParmId: String,
  @Value($$"${service-now.requestor}")private val requestor: String,
  @Value($$"${service-now.requested-for}")private val requestedFor: String,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/jobs/service-now/generate-delius-merge-requests"])
  fun collectAndReport(): String {
    getClustersForMergeRequests().forEach {
      val payload = ServiceNowMergeRequestPayload(
        sysParmId = sysParmId,
        quantity = 1,
        variables = Variables(
          requester = requestor,
          requestedFor = requestedFor,
          details = it.personEntities.map { person -> ProbationRecord.from(person) },
        ),
      )
      serviceNowMergeRequestClient.postRecords(payload)
      serviceNowMergeRequestRepository.save(ServiceNowMergeRequestEntity.fromUuid(it.personKeyUUID))
    }
    return "ok"
  }

  fun getClustersForMergeRequests(): List<MergeRequestItem> {
    val thisTimeYesterday = LocalDateTime.now().minusDays(1)
    val clustersWithMoreThanOneProbationRecordWhichWereChangedYesterday = personRepository.findByLastModifiedBetween(
      thisTimeYesterday,
      thisTimeYesterday.plusHours(1),
    )
      .filter { person ->
        person.personKey!!.personEntities.filter {
          it.sourceSystem == DELIUS
        }.size > 1
      }.map { it.personKey!! }
    val clustersWhichHaveNotAlreadyHadMergeRequest = clustersWithMoreThanOneProbationRecordWhichWereChangedYesterday
      .filterNot {
        serviceNowMergeRequestRepository.existsByPersonUUID(
          it.personUUID,
        )
      }
    return clustersWhichHaveNotAlreadyHadMergeRequest.toSet().map {
      MergeRequestItem(
        it.personUUID!!,
        it.personEntities.filter {
          it.sourceSystem == DELIUS
        },
      )
    }.take(CLUSTER_TO_PROCESS_COUNT)
  }

  data class MergeRequestItem(
    val personKeyUUID: UUID,
    val personEntities: List<PersonEntity>,
  )

  companion object {
    private const val CLUSTER_TO_PROCESS_COUNT = 5
  }
}
