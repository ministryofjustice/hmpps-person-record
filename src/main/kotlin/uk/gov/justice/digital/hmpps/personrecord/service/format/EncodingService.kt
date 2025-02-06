package uk.gov.justice.digital.hmpps.personrecord.service.format

import kotlinx.coroutines.runBlocking
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
  private val retryExecutor: RetryExecutor,
) {

  fun getProbationCase(crn: String): Result<ProbationCase?> = runCatching {
    runBlocking {
      retryExecutor.runWithRetryHTTP {
        corePersonRecordAndDeliusClient.getProbationCase(crn)
      }
    }
  }

  fun getPrisonerDetails(prisonNumber: String, onSuccess: (value: Prisoner?) -> Unit?, onFailure: (exception: Throwable) -> Unit?) = runCatching {
    runBlocking {
      retryExecutor.runWithRetryHTTP {
        prisonerSearchClient.getPrisoner(prisonNumber)
      }
    }
  }.fold(onSuccess, onFailure)
}
