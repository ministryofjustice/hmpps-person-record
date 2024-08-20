package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor

@Component
class BaseProbationEventProcessor(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
) {
  companion object {
    private const val MAX_RETRY_ATTEMPTS: Int = 3
    internal val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getProbationCase(crn: String, retryDelay: Long): Result<ProbationCase?> = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(MAX_RETRY_ATTEMPTS, retryDelay) {
        Result.success(corePersonRecordAndDeliusClient.getProbationCase(crn))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
