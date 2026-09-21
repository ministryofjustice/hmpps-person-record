package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import kotlin.time.Duration
import kotlin.time.measureTime

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
class PseudonymEthnicityMigrationJob(
  private val personRepository: PersonRepository,
  private val transactionalEthnicityUpdater: TransactionalEthnicityUpdater,
  @Value($$"${MIGRATION_START_PAGE:0}") private val startPage: Int,
  @Value($$"${MIGRATION_BATCH_SIZE:500}") private val batchSize: Int,
) : BatchJob {
  override val jobName = "PSEUDONYM_ETHNICITY_MIGRATION"

  override fun run() {
    val executionResults = forPage { page ->
      page.content.forEach { person ->
        transactionalEthnicityUpdater.update(person)
      }
    }
    log.info(jobName + " total elements: ${executionResults.totalElements}, elapsed time: ${executionResults.elapsedTime}")
  }

  private inline fun forPage(page: (Page<PersonEntity>) -> Unit): ExecutionResult {
    var pageNumber = startPage
    var personEntities: Page<PersonEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, batchSize, Sort.by("id"))
        personEntities = personRepository.findAllByEthnicityCodeIsNotNull(pageable)
        log.info("$jobName processing page ${personEntities.number + 1}/${personEntities.totalPages} (${personEntities.numberOfElements} elements)")
        page(personEntities)
        pageNumber++
      } while (personEntities.hasNext())
    }
    return ExecutionResult(
      totalPages = personEntities.totalPages,
      totalElements = personEntities.totalElements,
      elapsedTime = elapsedTime,
    )
  }

  private data class ExecutionResult(
    val totalPages: Int,
    val totalElements: Long,
    val elapsedTime: Duration,
  )

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
