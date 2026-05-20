package uk.gov.justice.digital.hmpps.personrecord.seeding

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams

@RestController
class PopulateFromProbationController(
  val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
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
      val totalPages = corePersonRecordAndDeliusClient.getProbationCases(
        CorePersonRecordAndDeliusClientPageParams(
          0,
          PAGE_SIZE,
        ),
      )?.page?.totalPages ?: 1

      log.info("Starting address updating, total pages: $totalPages")
      for (page in 0..<totalPages) {
        retryableProbationUpdater.repopulateProbationRecord(CorePersonRecordAndDeliusClientPageParams(page, PAGE_SIZE))
      }
      log.info("finished add seeding finished, approx records ${totalPages * PAGE_SIZE}")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val PAGE_SIZE = 1000
  }
}
