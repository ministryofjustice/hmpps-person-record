package uk.gov.justice.digital.hmpps.personrecord.seeding

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.launch
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
    CoroutineScope(Default).launch {
      val totalPages = corePersonRecordAndDeliusClient.getProbationCases(
        CorePersonRecordAndDeliusClientPageParams(
          0,
          config.pageSize,
        ),
      )?.page?.totalPages ?: 1

      log.info("Starting address updating, start page ${config.startPage} total pages (zero based): ${totalPages - 1}")
      for (page in config.startPage..<totalPages) {
        log.info("Page $page of ${totalPages - 1} start")
        retryableProbationUpdater.repopulateProbationRecord(CorePersonRecordAndDeliusClientPageParams(page, config.pageSize))
        log.info("Page $page of ${totalPages - 1} end ${config.pageSize * (page + 1)} records done of ${config.pageSize * totalPages}")
      }
      log.info("finished address updating, approximate records ${totalPages * config.pageSize - config.startPage * config.pageSize}")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class PopulateConfig(val startPage: Long = 0, val pageSize: Int = 500)
