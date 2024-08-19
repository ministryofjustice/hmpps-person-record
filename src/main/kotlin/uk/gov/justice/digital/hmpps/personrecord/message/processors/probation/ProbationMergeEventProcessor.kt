package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED

@Component
class ProbationMergeEventProcessor(
  val telemetryService: TelemetryService,
  val personRepository: PersonRepository,
  val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  @Value("\${retry.delay}")
  private val retryDelay: Long = 0,
) {
  companion object {
    private const val MAX_RETRY_ATTEMPTS: Int = 3
    internal val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(domainEvent: DomainEvent) {
    telemetryService.trackEvent(
      MERGE_MESSAGE_RECEIVED,
      mapOf(
        EventKeys.SOURCE_CRN to domainEvent.additionalInformation?.sourceCrn,
        EventKeys.TARGET_CRN to domainEvent.additionalInformation?.targetCrn,
        EventKeys.EVENT_TYPE to domainEvent.eventType,
        EventKeys.SOURCE_SYSTEM to DELIUS.name,
      ),
    )
    getProbationCase(domainEvent.additionalInformation?.targetCrn!!).fold(
      onSuccess = {
        log.info("Successfully mapped merge record")
      },
      onFailure = {
        log.error("Error retrieving offender detail: ${it.message}")
        throw it
      },
    )
  }

  private fun getProbationCase(crn: String): Result<ProbationCase?> = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(MAX_RETRY_ATTEMPTS, retryDelay) {
        Result.success(corePersonRecordAndDeliusClient.getProbationCase(crn))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
