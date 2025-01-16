package uk.gov.justice.digital.hmpps.personrecord.service.format

import kotlinx.coroutines.runBlocking
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
      RetryExecutor.runWithRetryHTTP(retryDelay) {
        corePersonRecordAndDeliusClient.getProbationCase(crn)
      }
    }
  }

  fun getPrisonerDetails(prisonNumber: String): Result<Prisoner?> = runCatching {
    runBlocking {
      RetryExecutor.runWithRetryHTTP(retryDelay) {
        prisonerSearchClient.getPrisoner(prisonNumber)
      }
    }
  }
}
