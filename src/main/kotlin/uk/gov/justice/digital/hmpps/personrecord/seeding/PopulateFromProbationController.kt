package uk.gov.justice.digital.hmpps.personrecord.seeding

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@RestController
class PopulateFromProbationController(
  val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  @Value("\${populate-from-probation.page-size}") val pageSize: Int,
  private val personRepository: PersonRepository,
  private val retryableProbationUpdater: RetryableProbationUpdater,

) {

  @Hidden
  @PostMapping(value = ["/admin/populate-from-probation"])
  suspend fun populate(): String {
    populatePages()
    return "OK"
  }

  suspend fun populatePages() {
    CoroutineScope(Dispatchers.Default).launch {
      // todo transactional
      val totalPages = corePersonRecordAndDeliusClient.getProbationCases(
        CorePersonRecordAndDeliusClientPageParams(
          0,
          pageSize,
        ),
      )?.page?.totalPages ?: 1

      log.info("Starting address updating, total pages: $totalPages")
      for (page in 0..<totalPages) {

        retryableProbationUpdater.repopulateProbationRecord(CorePersonRecordAndDeliusClientPageParams(page, pageSize))
      }
      log.info("finished add seeding finished, approx records ${totalPages * pageSize}")
    }
  }


  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
