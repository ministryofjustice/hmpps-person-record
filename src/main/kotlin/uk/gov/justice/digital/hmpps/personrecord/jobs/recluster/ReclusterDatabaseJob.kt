package uk.gov.justice.digital.hmpps.personrecord.jobs.recluster

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jobs.BatchJob
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.TransactionalReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import kotlin.time.Duration
import kotlin.time.measureTime

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
class ReclusterDatabaseJob(
  private val personRepository: PersonRepository,
  private val transactionalReclusterService: TransactionalReclusterService,
  private val publisher: ApplicationEventPublisher,
  @Value($$"${RECLUSTER_DATABASE_BATCH_SIZE:0}") private val batchSize: Int,
  @Value($$"${RECLUSTER_DATABASE_START_PAGE:0}") private val startPage: Int,
  @Value($$"${RECLUSTER_DATABASE_END_PAGE:2147483647}") private val endPageInclusive: Int,

) : BatchJob {
  override val jobName = "RECLUSTER_DATABASE"

  override fun run() {
    var pageNumber = startPage
    var page: Page<PersonEntity>
    if (startPage >= endPageInclusive) {
      throw IllegalArgumentException("End page (inclusive) '$endPageInclusive' must be greater than start page '$startPage'")
    }
    LOG.info("Starting '$jobName' with parameters: start page '$startPage', end page '$endPageInclusive' and batch size '$batchSize'")

    var hasNotCompletedEndPage = true
    val fullJobTime: Duration = measureTime {
      do {
        LOG.info("Starting re-clustering of persons on page '$pageNumber' with batch size '$batchSize'")
        val pageTime = measureTime {
          val pageable = PageRequest.of(pageNumber, batchSize, Sort.by("id"))
          page = personRepository.findAllByMergedToNullAndPassiveState(false, pageable)
          reclusterPersons(page.content)
        }
        LOG.info("Time taken for page '$pageNumber' to complete '${pageTime.inWholeSeconds}' seconds")
        if (pageNumber >= endPageInclusive) {
          hasNotCompletedEndPage = false
        }

        pageNumber++
      } while (page.hasNext() && hasNotCompletedEndPage)
    }

    LOG.info("Total run time of '$jobName': '${fullJobTime.inWholeSeconds}' seconds")
  }

  private fun reclusterPersons(persons: List<PersonEntity>) {
    persons.forEach { person ->
      publisher.publishEvent(RecordClusterTelemetry(TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED, person.personKey!!))
      transactionalReclusterService.recluster(person)
    }
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(ReclusterDatabaseJob::class.java)
  }
}
