package uk.gov.justice.digital.hmpps.personrecord.seeding

import jakarta.transaction.Transactional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchMigrateRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import kotlin.time.Duration
import kotlin.time.measureTime

private const val OK = "OK"

@RestController
class PopulatePersonMatch(
  private val personRepository: PersonRepository,
  @Value("\${populate-person-match.batch-size}") val batchSize: Int,
  private val personMatchClient: PersonMatchClient,
  private val retryExecutor: RetryExecutor,
) {

  @RequestMapping(method = [RequestMethod.POST], value = ["/populatepersonmatch"])
  suspend fun populate(): String {
    runPopulation()
    return OK
  }

  @Transactional
  suspend fun runPopulation() {
    CoroutineScope(Dispatchers.Default).launch {
      log.info("Starting population of person-match")
      val executionResults = forPage { page ->
        log.info("Populating person match, page: ${page.pageable.pageNumber + 1}")
        val personMatchRecords = page.content.map { PersonMatchRecord.from(it) }
        val personMatchMigrateRequest = PersonMatchMigrateRequest(records = personMatchRecords)
        retryExecutor.runWithRetryHTTP { personMatchClient.postPersonMigrate(personMatchMigrateRequest) }
      }
      log.info(
        "Finished populating person-match, total pages: ${executionResults.totalPages}, " +
          "total elements: ${executionResults.totalElements}, " +
          "elapsed time: ${executionResults.elapsedTime}",
      )
    }
  }

  private inline fun forPage(page: (Page<PersonEntity>) -> Unit): ExecutionResult {
    var pageNumber = 0
    var personRecords: Page<PersonEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, batchSize)

        personRecords = personRepository.findAll(pageable)
        page(personRecords)

        pageNumber++
      } while (personRecords.hasNext())
    }
    return ExecutionResult(
      totalPages = personRecords.totalPages,
      totalElements = personRecords.totalElements,
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
