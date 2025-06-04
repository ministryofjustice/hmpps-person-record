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

  fun getProbationCase(crn: String, onSuccess: (value: ProbationCase?) -> Any?) = runCatching {
    runBlocking {
      retryExecutor.runWithRetryHTTP {
        corePersonRecordAndDeliusClient.getProbationCase(crn)
      }
    }
  }.fold(onSuccess, failedToRetrieveProbationCase())

  fun getPrisonerDetails(prisonNumber: String, onSuccess: (value: Prisoner?) -> Any?) = runCatching {
    prisonerSearchClient.getPrisoner(prisonNumber)
  }.fold(onSuccess, failedToRetrievePrisoner())

  private fun failedToRetrievePrisoner(): (exception: Throwable) -> Unit? = {
    log.error("Error retrieving prisoner detail: ${it.message}")
    throw it
  }

  private fun failedToRetrieveProbationCase(): (exception: Throwable) -> Unit? = {
    log.error("Error retrieving new offender detail: ${it.message}")
    throw it
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
