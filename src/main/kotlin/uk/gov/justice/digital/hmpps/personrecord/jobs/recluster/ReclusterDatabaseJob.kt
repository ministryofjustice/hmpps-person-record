package uk.gov.justice.digital.hmpps.personrecord.jobs.recluster

import org.slf4j.LoggerFactory
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
) : BatchJob {
  override val jobName = "RECLUSTER_DATABASE"

  override fun run() {
    var pageNumber = START_PAGE_NUMBER
    var personEntities: Page<PersonEntity>
    LOG.info("Starting '$jobName' with parameters: page number '$START_PAGE_NUMBER' and batch size '$BATCH_SIZE'")

    val fullJobTime: Duration = measureTime {
      do {
        LOG.info("Starting re-clustering of persons on page '$pageNumber' with batch size '$BATCH_SIZE'")
        val pageTime = measureTime {
          val pageable = PageRequest.of(pageNumber, BATCH_SIZE, Sort.by("id"))
          personEntities = personRepository.findAll(pageable)
          val personsNotMergedOrPassive = personEntities.content.filter { it.personKey != null && !it.isPassive() }
          reclusterPersons(personsNotMergedOrPassive)
          pageNumber++
        }
        LOG.info("Time taken for page '$pageNumber' to complete '${pageTime.inWholeSeconds}' seconds")
      } while (personEntities.hasNext())
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
    private val BATCH_SIZE = 50
    private val START_PAGE_NUMBER = 0
  }
}
