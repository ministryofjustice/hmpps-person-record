package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor

@Component
class BasePrisonEventProcessor {

  @Autowired
  private lateinit var prisonerSearchClient: PrisonerSearchClient

  @Value("\${retry.delay}")
  private val retryDelay: Long = 0

  companion object {
    private const val MAX_RETRY_ATTEMPTS: Int = 3
    internal val log = LoggerFactory.getLogger(this::class.java)
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
}
