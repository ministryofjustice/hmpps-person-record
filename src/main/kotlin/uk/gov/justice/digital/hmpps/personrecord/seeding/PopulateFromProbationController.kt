package uk.gov.justice.digital.hmpps.personrecord.seeding

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hibernate.query.Page.page
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
  suspend fun populate(@RequestBody config: PopulateConfig): String {
    populatePages(config)
    return "OK"
  }

  suspend fun populatePages(config: PopulateConfig) {
    CoroutineScope(Dispatchers.Default).launch {
      val totalPages = corePersonRecordAndDeliusClient.getProbationCases(
        CorePersonRecordAndDeliusClientPageParams(
          0,
          PAGE_SIZE,
        ),
      )?.page?.totalPages ?: 1

      log.info("Starting address updating, start page ${config.startPage} total pages: $totalPages")
      for (page in config.startPage..<totalPages) {
        log.info("Page $page start")
        retryableProbationUpdater.repopulateProbationRecord(CorePersonRecordAndDeliusClientPageParams(page, PAGE_SIZE))
        log.info("Page $page end ${PAGE_SIZE * (page + 1)} records done of ${PAGE_SIZE * totalPages}")
      }
      log.info("finished address updating, approximate records ${totalPages * PAGE_SIZE - config.startPage * PAGE_SIZE}")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val PAGE_SIZE = 1000
  }
}

data class PopulateConfig(val startPage: Long = 0)
