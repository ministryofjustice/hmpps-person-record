package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import java.time.LocalDateTime
import java.util.UUID

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
    log.info("finished getting modified clusters for ${recordsModifiedYesterday.size}")
    val clusters = recordsModifiedYesterday
      .distinctBy { it.personKey }

    log.info("Got distinct ${clusters.size}")

    val clustersWithMoreThanOneProbationRecord = clusters.filter { hasMoreThanOneProbationRecord(it) }
    log.info("Found ${clustersWithMoreThanOneProbationRecord.size} clusters with more than one probation record")

    val clustersWithNoExistingMergeRequest = clustersWithMoreThanOneProbationRecord.filterNot { mergeRequestAlreadyMade(it.personKey!!.personUUID!!) }
    log.info("removed ${clustersWithNoExistingMergeRequest.size} requests already made")
    val mergeRequestItems = clustersWithNoExistingMergeRequest.take(CLUSTER_TO_PROCESS_COUNT).map {
      log.info(
        "probation records on cluster: ${
          it.personKey!!.personEntities.filter { person ->
            person.isProbationRecord()
          }.size
        }",
      )
      MergeRequestItem(
        it.personKey!!.personUUID!!,
        it.personKey!!.personEntities.filter { person ->
          person.isProbationRecord()
        }.map { person -> MergeRequestDetails.from(person) },
      )
    }
    log.info("Merge requests to be made:  ${mergeRequestItems.size}")
    return mergeRequestItems
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
    private const val HOURS_TO_CHOOSE_FROM = 10L
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
