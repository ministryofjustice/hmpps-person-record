package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams
import uk.gov.justice.digital.hmpps.personrecord.jobs.migration.RetryableProbationUpdater

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
class LowercasePncCroMigrationJob(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val retryableProbationUpdater: RetryableProbationUpdater,
) : BatchJob {
  override val jobName = "LOWERCASE_PNC_CRO_MIGRATION"
  private val startPage: Int = 0
  private val pageSize: Int = 500

  override fun run() {
    val totalPages = corePersonRecordAndDeliusClient.getProbationCases(
      CorePersonRecordAndDeliusClientPageParams(
        0,
        pageSize,
      ),
    )?.page?.totalPages ?: 1

    log.info("Starting lowercase PNC & CRO migration, start page $startPage total pages (zero based): ${totalPages - 1}")
    for (page in startPage..<totalPages) {
      log.info("Page $page of ${totalPages - 1} start")
      retryableProbationUpdater.repopulateProbationRecord(CorePersonRecordAndDeliusClientPageParams(page, pageSize))
      log.info("Page $page of ${totalPages - 1} end ${pageSize * (page + 1)} records done of ${pageSize * totalPages}")
    }
    log.info("Finished lowercase PNC & CRO migration, approximate records ${totalPages * pageSize - startPage * pageSize}")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
