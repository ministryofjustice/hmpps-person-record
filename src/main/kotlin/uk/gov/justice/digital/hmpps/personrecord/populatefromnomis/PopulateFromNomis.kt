package uk.gov.justice.digital.hmpps.personrecord.populatefromnomis

import feign.FeignException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import uk.gov.justice.digital.hmpps.personrecord.client.PageParams
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonServiceClient
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerNumbers
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry

private const val OK = "OK"
private const val TEMPORARY_LIMIT = 4

private val retryables = listOf(HttpClientErrorException::class, HttpServerErrorException::class, FeignException.InternalServerError::class, FeignException.ServiceUnavailable::class)

@RestController
class PopulateFromNomis(
  val prisonerSearchClient: PrisonerSearchClient,
  val prisonServiceClient: PrisonServiceClient,
  @Value("\${populate-from-nomis.page-size}") val pageSize: Int,
  @Value("\${populate-from-nomis.retry.delay}") val delayMillis: Long,
  @Value("\${populate-from-nomis.retry.times}") val retries: Int,
  val repository: PersonRepository,
) {

  @RequestMapping(method = [RequestMethod.POST], value = ["/populatefromnomis"])
  suspend fun populate(): String {
    populatePages()
    return OK
  }

  suspend fun populatePages() {
    CoroutineScope(Dispatchers.Default).launch {
      // if this call fails we will just restart the process, no need to retry
      val prisonerNumbers = prisonServiceClient.getPrisonerNumbers(PageParams(0, pageSize))!!
      val totalPages = prisonerNumbers.totalPages
      var numbers = prisonerNumbers.numbers

      for (page in 1..TEMPORARY_LIMIT) { // totalPages but just doing first 4 pages = 4000 records to see how it performs
        runWithRetry(retryables, retries, delayMillis) {
          val prisoners = prisonerSearchClient.getPrisoners(PrisonerNumbers(numbers))
          prisoners.forEach {
            val person = Person.from(it)
            val personToSave = PersonEntity.from(person)
            repository.saveAndFlush(personToSave)
          }
        }

        // don't really like this, but it saves 1 call to getPrisonerNumbers
        if (page < totalPages) {
          runWithRetry(retryables, retries, delayMillis) {
            numbers = prisonServiceClient.getPrisonerNumbers(PageParams(page, pageSize))!!.numbers
          }
        }
      }
    }
  }
}
