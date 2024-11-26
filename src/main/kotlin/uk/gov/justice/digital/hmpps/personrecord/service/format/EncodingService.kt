package uk.gov.justice.digital.hmpps.personrecord.service.format

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor

@Component
class EncodingService(
  private var corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private var prisonerSearchClient: PrisonerSearchClient,
  @Value("\${retry.delay}")
  private val retryDelay: Long = 0,
) {

  fun getProbationCase(crn: String): Result<ProbationCase?> = runCatching {
    runBlocking {
      RetryExecutor.runWithRetry(MAX_RETRY_ATTEMPTS, retryDelay) {
        corePersonRecordAndDeliusClient.getProbationCase(crn)
      }
    }
  }

  fun getPrisonerDetails(prisonNumber: String): Result<Prisoner?> = runCatching {
    runBlocking {
      RetryExecutor.runWithRetry(MAX_RETRY_ATTEMPTS, retryDelay) {
        prisonerSearchClient.getPrisoner(prisonNumber)
      }
    }
  }

  companion object {
    private const val MAX_RETRY_ATTEMPTS: Int = 3
    internal val log = LoggerFactory.getLogger(this::class.java)
  }
}
