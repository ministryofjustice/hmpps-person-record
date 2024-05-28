package uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

const val MAX_RETRY_ATTEMPTS: Int = 3

@Service
class PrisonerEventsProcessor(
  val telemetryService: TelemetryService,
  val prisonerSearchClient: PrisonerSearchClient,
) {
  companion object {
    @Value("\${retry.delay}")
    private val retryDelay: Long = 0
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(domainEvent: DomainEvent) {
    val nomsNumber = domainEvent.additionalInformation?.nomsNumber ?: ""
    telemetryService.trackEvent(
      TelemetryEventType.DOMAIN_EVENT_RECEIVED,
      mapOf("eventType" to domainEvent.eventType, "NOMS_NUMBER" to nomsNumber, "SourceSystem" to SourceSystemType.NOMIS.name),
    )
    getPrisonerDetails(nomsNumber).fold(
      onSuccess = {
        // To be completed in CPR-296
        log.info("Not processing nomis message")
      },
      onFailure = {
        log.error("Error retrieving prisoner detail: ${it.message}")
        throw it
      },
    )
  }

  private fun getPrisonerDetails(nomsNumber: String): Result<Prisoner?> = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(MAX_RETRY_ATTEMPTS, retryDelay) {
        Result.success(prisonerSearchClient.getPrisoner(nomsNumber))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
