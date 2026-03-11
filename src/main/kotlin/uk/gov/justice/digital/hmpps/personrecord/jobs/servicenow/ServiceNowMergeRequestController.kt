package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import io.swagger.v3.oas.annotations.Hidden
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import java.time.LocalDateTime
import java.util.UUID

@RestController
class ServiceNowMergeRequestController(
  private val personRepository: PersonRepository,
  private val serviceNowMergeRequestClient: ServiceNowMergeRequestClient,
  private val serviceNowMergeRequestRepository: ServiceNowMergeRequestRepository,
  @Value($$"${service-now.sysparm-id}") private val sysParmId: String,
  @Value($$"${service-now.requestor}") private val requestor: String,
  @Value($$"${service-now.requested-for}") private val requestedFor: String,
  private val jsonMapper: JsonMapper,
) {

  @Hidden
  @Transactional
  @RequestMapping(method = [POST], value = ["/jobs/service-now/generate-delius-merge-requests"])
  fun collectAndReport(): String {
    // TODO ignore merged records
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
    return "ok"
  }

  fun getClustersForMergeRequests(): List<MergeRequestItem> {
    log.info("starting")
    val thisTimeYesterday = LocalDateTime.now().minusDays(1)
    val findByLastModifiedAfter = personRepository.findByLastModifiedBetween(
      thisTimeYesterday,
      thisTimeYesterday.plusHours(HOURS_TO_CHOOSE_FROM),
    )
    log.info("finished getting modified clusters for ${findByLastModifiedAfter.size}")
    val distinctBy = findByLastModifiedAfter
      .distinctBy { it.personKey }

    log.info("Got distinct ${distinctBy.size}")

    val dupes = distinctBy.filter { hasMoreThanOneProbationRecord(it) }
    log.info("Got duplicates ${dupes.size}")

    val noDoubles = dupes.filterNot { mergeRequestAlreadyMade(it.personKey!!.personUUID!!) }
    log.info("removed requests already made ${noDoubles.size}")
    val five = noDoubles.take(CLUSTER_TO_PROCESS_COUNT)
    log.info("taken five ${five.size}")
    val mapped = five.map {
      log.info(
        " probation records ${it.personKey!!.personEntities.filter { person ->
          person.isProbationRecord()
        }.size}",
      )
      MergeRequestItem(
        it.personKey!!.personUUID!!,
        it.personKey!!.personEntities.filter { person ->
          person.isProbationRecord()
        }.map { person -> MergeRequestDetails.from(person) },
      )
    }
    log.info("mapped ${mapped.size}")
    return mapped
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
    private const val HOURS_TO_CHOOSE_FROM = 1L
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
