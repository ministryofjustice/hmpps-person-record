package uk.gov.justice.digital.hmpps.personrecord.service.format

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
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

  fun getPrisonerDetails(prisonNumber: String, onSuccess: (value: Prisoner?) -> Unit?) = runCatching {
    runBlocking {
      retryExecutor.runWithRetryHTTP {
        prisonerSearchClient.getPrisoner(prisonNumber)
      }
    }
  }.fold(onSuccess, failedToRetrieve())

  private fun failedToRetrieve(): (exception: Throwable) -> Unit? = {
    log.error("Error retrieving prisoner detail: ${it.message}")
    throw it
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
