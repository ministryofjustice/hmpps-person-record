package uk.gov.justice.digital.hmpps.personrecord.seeding

import jakarta.transaction.Transactional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor

private const val OK = "OK"

@RestController
class PopulatePersonMatch(
  private val personRepository: PersonRepository,
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
      val (totalPages, totalElements) = forPage { page ->
        val personMatchRecords = page.content.map { PersonMatchRecord.from(it) }
        val personMatchRequest = PersonMatchRequest(records = personMatchRecords)
        retryExecutor.runWithRetryHTTP { personMatchClient.postPersonMigrate(personMatchRequest) }
      }
      log.info("Finished populating person-match, total pages: $totalPages, total elements: $totalElements")
    }
  }

  private inline fun forPage(page: (Page<PersonEntity>) -> Unit): Pair<Int, Long> {
    var pageNumber = 0
    var personRecords: Page<PersonEntity>
    do {
      log.info("Populating person match, page: $pageNumber")
      val pageable = PageRequest.of(pageNumber, BATCH_SIZE)

      personRecords = personRepository.findAll(pageable)
      page(personRecords)

      pageNumber++
    } while (personRecords.hasNext())
    return Pair(personRecords.totalPages, personRecords.totalElements)
  }

  companion object {
    private const val BATCH_SIZE = 1000
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
