package uk.gov.justice.digital.hmpps.personrecord.seeding

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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry

private const val OK = "OK"

@RestController
@Profile("seeding")
class PopulateFromPrison(
  val prisonerSearchClient: PrisonerSearchClient,
  val prisonServiceClient: PrisonServiceClient,
  @Value("\${populate-from-nomis.page-size}") val pageSize: Int,
  @Value("\${populate-from-nomis.retry.delay}") val delayMillis: Long,
  @Value("\${populate-from-nomis.retry.times}") val retries: Int,
  val repository: PersonRepository,
) {

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
        runWithRetry(retries, delayMillis) {
          prisonerSearchClient.getPrisonNumbers(PrisonNumbers(numbers))
        }?.forEach {
          val person = Person.from(it)
          val personToSave = PersonEntity.from(person)
          repository.saveAndFlush(personToSave)
        }

        // don't really like this, but it saves 1 call to getPrisonerNumbers
        if (page < totalPages) {
          runWithRetry(retries, delayMillis) {
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
