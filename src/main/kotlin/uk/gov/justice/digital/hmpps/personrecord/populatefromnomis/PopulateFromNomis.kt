package uk.gov.justice.digital.hmpps.personrecord.populatefromnomis

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.PageParams
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonServiceClient
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository

private const val OK = "OK"

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
    CoroutineScope(Job()).launch {
      val prisonerNumbers = prisonServiceClient.getPrisonerNumbers(PageParams(0, pageSize))!!
      val totalPages = prisonerNumbers.totalPages // is this 0 based?
      var numbers = prisonerNumbers.numbers

      for (page in 1..totalPages) {
        numbers.forEach {
          val prisoner = prisonerSearchClient.getPrisoner(it)

          val prisonerEntity = DefendantEntity(firstName = prisoner.firstName)
          defendantRepository.save(prisonerEntity)
        }
        if (page < totalPages) {
          // don't really like this but it saves 1 call to getPrisonerNumbers
          numbers = prisonServiceClient.getPrisonerNumbers(PageParams(page, pageSize))!!.numbers
        }
      }
    }
  }
}
