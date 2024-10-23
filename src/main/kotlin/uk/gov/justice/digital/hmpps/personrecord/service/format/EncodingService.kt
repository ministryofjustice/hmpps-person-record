package uk.gov.justice.digital.hmpps.personrecord.service.format

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor

@Service
class EncodingService(
  private var corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private var prisonerSearchClient: PrisonerSearchClient,
) {

  @Value("\${retry.delay}")
  private val retryDelay: Long = 0

  fun getProbationCase(crn: String): Result<ProbationCase?> = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(MAX_RETRY_ATTEMPTS, retryDelay) {
        Result.success(corePersonRecordAndDeliusClient.getProbationCase(crn))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun getPrisonerDetails(prisonNumber: String): Result<Prisoner?> = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(MAX_RETRY_ATTEMPTS, retryDelay) {
        Result.success(prisonerSearchClient.getPrisoner(prisonNumber))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  companion object {
    private const val MAX_RETRY_ATTEMPTS: Int = 3
    internal val log = LoggerFactory.getLogger(this::class.java)
  }
}
