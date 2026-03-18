package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.min

@Profile("!prod")
@Component
class ServiceNowMergeRequestService(
  private val personRepository: PersonRepository,
  private val serviceNowMergeRequestClient: ServiceNowMergeRequestClient,
  private val serviceNowMergeRequestRepository: ServiceNowMergeRequestRepository,
  @Value($$"${service-now.sysparm-id}") private val sysParmId: String,
  @Value($$"${service-now.requestor}") private val requestor: String,
  @Value($$"${service-now.requested-for}") private val requestedFor: String,
  private val jsonMapper: JsonMapper,
) {

  @Transactional
  fun process() {
    getClustersForMergeRequests().forEach {
      val payload = ServiceNowMergeRequestPayload(
        sysParmId = sysParmId,
        variables = Variables(
          requestor = requestor,
          requestedFor = requestedFor,
          details = jsonMapper.writeValueAsString(it.mergeRequestDetails),
        ),
      )
      serviceNowMergeRequestClient.postRecords(payload)
      serviceNowMergeRequestRepository.save(ServiceNowMergeRequestEntity.fromUuid(it.personKeyUUID))
    }
  }

  fun getClustersForMergeRequests(): List<MergeRequestItem> {
    log.info("starting")
    val thisTimeYesterday = LocalDateTime.now().minusDays(1)
    val recordsModifiedYesterday = personRepository.findByLastModifiedBetween(
      thisTimeYesterday,
      thisTimeYesterday.plusHours(HOURS_TO_CHOOSE_FROM),
    )
    log.info("finished getting ${recordsModifiedYesterday.size} modified records")
    val clusters = recordsModifiedYesterday
      .distinctBy { it.personKey }

    log.info("Got ${clusters.size} distinct clusters")

    val clustersWithMoreThanOneProbationRecord = clusters.filter { hasMoreThanOneProbationRecord(it) }
    log.info("Found ${clustersWithMoreThanOneProbationRecord.size} clusters with more than one probation record")

    val clustersWithNoExistingMergeRequest = clustersWithMoreThanOneProbationRecord.filterNot { mergeRequestAlreadyMade(it.personKey!!.personUUID!!) }
    log.info("Removed ${clustersWithMoreThanOneProbationRecord.size - clustersWithNoExistingMergeRequest.size} requests already made")
    val requestsCount = min(CLUSTER_TO_PROCESS_COUNT, clustersWithNoExistingMergeRequest.size)
    log.info("Sending $requestsCount requests")
    return clustersWithNoExistingMergeRequest.take(CLUSTER_TO_PROCESS_COUNT).map {
      log.info(
        "Number of probation records on cluster: ${
          it.personKey!!.personEntities.count { person ->
            person.isProbationRecord()
          }
        }",
      )
      MergeRequestItem(
        it.personKey!!.personUUID!!,
        it.personKey!!.personEntities.filter { person ->
          person.isProbationRecord()
        }.map { person -> MergeRequestDetails.from(person) },
      )
    }
  }

  private fun hasMoreThanOneProbationRecord(person: PersonEntity): Boolean = person.personKey?.let { personKeyEntity -> personKeyEntity.personEntities.count { it.isProbationRecord() } > 1 } ?: false

  private fun PersonEntity.isProbationRecord(): Boolean = this.sourceSystem == DELIUS

  private fun mergeRequestAlreadyMade(personUUID: UUID): Boolean = serviceNowMergeRequestRepository.existsByPersonUUID(personUUID)

  data class MergeRequestItem(
    val personKeyUUID: UUID,
    val mergeRequestDetails: List<MergeRequestDetails>,
  )

  companion object {
    private const val CLUSTER_TO_PROCESS_COUNT = 5
    private const val HOURS_TO_CHOOSE_FROM = 10L
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
