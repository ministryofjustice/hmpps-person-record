package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import java.time.LocalDateTime
import java.util.UUID

@Profile("!preprod & !prod")
@Component
class ServiceNowMergeRequestService(
  private val personRepository: PersonRepository,
  private val serviceNowMergeRequestClient: ServiceNowMergeRequestClient,
  private val serviceNowMergeRequestRepository: ServiceNowMergeRequestRepository,
  @Value($$"${service-now.sysparm-id}") private val sysParmId: String,
  @Value($$"${service-now.requestor}") private val requestor: String,
  @Value($$"${service-now.requested-for}") private val requestedFor: String,
) {
  @Transactional
   fun sendMergeRequest() {
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
  }
  fun getClustersForMergeRequests(): List<MergeRequestItem> {
    log.info("starting")
    // val start = LocalDateTime.now().minusHours(HOURS_AGO)
    // there are 11 in this hour on dev which should be enough for testing.
    // Takes about 40 seconds to get them from the read replica.
    // we will replace this with the line above once we have proved it in dev
    // in preprod and prod there should be plenty every hour
    val start = START
    val findByLastModifiedAfter = personRepository.findByLastModifiedBetween(
      start,
      start.plusHours(HOURS_TO_CHOOSE_FROM),
    )
    log.info("finished getting modified clusters for $start")
    val distinctBy = findByLastModifiedAfter
      .distinctBy { it.personKey }

    log.info("Got distinct")

    val dupes = distinctBy.filter { hasMoreThanOneProbationRecord(it) }
    log.info("Got duplicates")

    val noDoubles = dupes.filterNot { mergeRequestAlreadyMade(it.personKey!!.personUUID!!) }
    log.info("removed requests already made")
    val five = noDoubles.take(CLUSTER_TO_PROCESS_COUNT)
    log.info("taken five")
    return five.map {
      MergeRequestItem(
        it.personKey!!.personUUID!!,
        it.personKey!!.personEntities.filter { person ->
          person.isProbationRecord()
        }.map { person -> MergeRequestDetails.from(person) },
      )
    }
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
    val START = LocalDateTime.of(2026, 2, 2, 14, 0)
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
