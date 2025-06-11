package uk.gov.justice.digital.hmpps.personrecord.seeding

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.PageParams
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonNumbers
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonServiceClient
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECORD_SEEDED

private const val OK = "OK"

@RestController
@Profile("seeding")
class PopulateFromPrison(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonServiceClient: PrisonServiceClient,
  @Value("\${populate-from-nomis.page-size}") val pageSize: Int,
  private val repository: PersonRepository,
  private val retryExecutor: RetryExecutor,
  private val eventLogRepository: EventLogRepository,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/populatefromprison"])
  suspend fun populate(): String {
    populatePages()
    return OK
  }

  suspend fun populatePages() {
    CoroutineScope(Dispatchers.Default).launch {
      // if this call fails we will just restart the process, no need to retry
      val prisonNumbers = prisonServiceClient.getPrisonNumbers(PageParams(0, pageSize))!!
      val totalPages = prisonNumbers.totalPages
      var numbers = prisonNumbers.numbers

      log.info("Starting Prison seeding, total pages: $totalPages")
      for (page in 1..totalPages) {
        log.info("Processing Prison seeding, page: $page / $totalPages")
        retryExecutor.runWithRetryHTTP {
          prisonerSearchClient.getPrisonNumbers(PrisonNumbers(numbers))
        }?.map {
          val person = Person.from(it)
          PersonEntity.new(person)
        }?.let { repository.saveAll(it) }
          ?.map { EventLogEntity.from(it, CPR_RECORD_SEEDED) }
          ?.let {
            eventLogRepository.saveAll(it)
          }

        // don't really like this, but it saves 1 call to getPrisonNumbers
        if (page < totalPages) {
          retryExecutor.runWithRetryHTTP {
            numbers = prisonServiceClient.getPrisonNumbers(PageParams(page, pageSize))!!.numbers
          }
        }
      }
      log.info("Prison seeding finished, approx records ${totalPages * pageSize}")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
