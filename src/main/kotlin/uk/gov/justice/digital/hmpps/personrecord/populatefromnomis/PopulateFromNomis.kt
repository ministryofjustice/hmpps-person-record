package uk.gov.justice.digital.hmpps.personrecord.populatefromnomis

import feign.FeignException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import uk.gov.justice.digital.hmpps.personrecord.client.PageParams
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonServiceClient
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor

private const val OK = "OK"

private const val RETRIES = 10
private const val DELAY_MILLIS = 5000L

private val retryables = listOf(HttpClientErrorException::class, HttpServerErrorException::class, FeignException.InternalServerError::class)

@RestController
class PopulateFromNomis(
  val prisonerSearchClient: PrisonerSearchClient,
  val prisonServiceClient: PrisonServiceClient,
  @Value("\${populate-from-nomis.page-size}") val pageSize: Int,
  val defendantRepository: DefendantRepository,
) {

  @RequestMapping(method = [RequestMethod.POST], value = ["/populatefromnomis"])
  suspend fun populate(): String {
    populatePages()
    return OK
  }

  suspend fun populatePages() {
    // TODO understand scoping here, how many threads?
    CoroutineScope(Job()).launch {
      // if this call fails we will just restart the process, no need to retry
      val prisonerNumbers = prisonServiceClient.getPrisonerNumbers(PageParams(0, pageSize))!!
      val totalPages = prisonerNumbers.totalPages // is this 0 based?
      // Code assumes that it is 1 based.
      // I have not been able to verify this from API docs or code
      var numbers = prisonerNumbers.numbers

      for (page in 1..totalPages) {
        numbers.forEach {
          // TODO retries
          val prisoner = prisonerSearchClient.getPrisoner(it)

          val prisonerEntity = DefendantEntity(firstName = prisoner.firstName)
          defendantRepository.save(prisonerEntity)
        }
        // don't really like this, but it saves 1 call to getPrisonerNumbers
        if (page < totalPages) {
          RetryExecutor.runWithRetry(retryables, RETRIES, DELAY_MILLIS) {
            numbers = prisonServiceClient.getPrisonerNumbers(PageParams(page, pageSize))!!.numbers
          }
        }
      }
    }
  }
}