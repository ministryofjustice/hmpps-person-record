package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
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
  @Value($$"${service-now.sysparm-id}") private val sysParmId: String,
  @Value($$"${service-now.requestor}") private val requestor: String,
  @Value($$"${service-now.requested-for}") private val requestedFor: String,
) {

  @Hidden
  @Transactional
  @RequestMapping(method = [POST], value = ["/jobs/service-now/generate-delius-merge-requests"])
  fun collectAndReport(): String {
    getClustersForMergeRequests().forEach {
      val payload = ServiceNowMergeRequestPayload(
        sysParmId = sysParmId,
        quantity = 1,
        variables = Variables(
          requester = requestor,
          requestedFor = requestedFor,
          details = it.mergeRequestDetails,
        ),
      )
      serviceNowMergeRequestClient.postRecords(payload)
      serviceNowMergeRequestRepository.save(ServiceNowMergeRequestEntity.fromUuid(it.personKeyUUID))
    }
    return "ok"
  }

  fun getClustersForMergeRequests(): List<MergeRequestItem> {
    val thisTimeYesterday = LocalDateTime.now().minusDays(1)
    return personRepository.findByLastModifiedBetween(
      thisTimeYesterday,
      thisTimeYesterday.plusHours(1),
    )
      .distinctBy { it.personKey }
      .filter { hasMoreThanOneProbationRecord(it) }
      .map {
        MergeRequestItem(
          it.personKey!!.personUUID!!,
          it.personKey!!.personEntities.filter { person ->
            person.isProbationRecord()
          }.map { person -> MergeRequestDetails.from(person) },
        )
      }
      .filterNot { mergeRequestAlreadyMade(it.personKeyUUID) }
      .take(CLUSTER_TO_PROCESS_COUNT)
  }

  private fun hasMoreThanOneProbationRecord(person: PersonEntity): Boolean = person.personKey!!.personEntities.count { it.isProbationRecord() } > 1

  private fun PersonEntity.isProbationRecord(): Boolean = this.sourceSystem == DELIUS

  private fun mergeRequestAlreadyMade(personUUID: UUID): Boolean = serviceNowMergeRequestRepository.existsByPersonUUID(personUUID)

  data class MergeRequestItem(
    val personKeyUUID: UUID,
    val mergeRequestDetails: List<MergeRequestDetails>,
  )

  companion object {
    private const val CLUSTER_TO_PROCESS_COUNT = 5
  }
}
