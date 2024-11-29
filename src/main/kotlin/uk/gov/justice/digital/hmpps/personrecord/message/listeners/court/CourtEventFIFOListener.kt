package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.DEFENDANT_ID
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.FIFO
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.MESSAGE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.FIFO_DEFENDANT_RECEIVED
const val MAX_RETRY_ATTEMPTS: Int = 3
const val CPR_COURT_EVENTS_FIFO_QUEUE_CONFIG_KEY = "cprcourteventsfifoqueue"

@Component
@Profile("!dev")
class CourtEventFIFOListener(
  private val objectMapper: ObjectMapper,
  private val telemetryService: TelemetryService,
  private val tempHearingService: TempHearingService,
  @Value("\${retry.delay}")
  private val retryDelay: Long = 0,
) {

  @SqsListener(CPR_COURT_EVENTS_FIFO_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(rawMessage: String) {
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
    when (sqsMessage.getMessageType()) {
      MessageType.COMMON_PLATFORM_HEARING.name -> processCommonPlatformHearingEvent(sqsMessage)
      else -> processLibraEvent(sqsMessage)
    }
  }

  private fun processCommonPlatformHearingEvent(sqsMessage: SQSMessage) {
    val commonPlatformHearingEvent = objectMapper.readValue<CommonPlatformHearingEvent>(
      sqsMessage.message,
    )
    try {
      runBlocking {
        RetryExecutor.runWithRetry(MAX_RETRY_ATTEMPTS, retryDelay) {
          tempHearingService.saveHearing(sqsMessage, commonPlatformHearingEvent)
        }
      }
    } catch (e: Exception) {
      log.error(e.message)
    }

    val uniqueDefendants = commonPlatformHearingEvent.hearing.prosecutionCases
      .flatMap { it.defendants }
      .distinctBy {
        it.id
      }
    uniqueDefendants.forEach {
      telemetryService.trackEvent(
        FIFO_DEFENDANT_RECEIVED,
        mapOf(
          SOURCE_SYSTEM to SourceSystemType.COMMON_PLATFORM.name,
          DEFENDANT_ID to it.id,
          EVENT_TYPE to sqsMessage.getMessageType(),
          MESSAGE_ID to sqsMessage.messageId,
          FIFO to "true",
        ),
      )
    }
  }

  private fun processLibraEvent(sqsMessage: SQSMessage) {
    telemetryService.trackEvent(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(

        EVENT_TYPE to MessageType.LIBRA_COURT_CASE.name,
        MESSAGE_ID to sqsMessage.messageId,
        SOURCE_SYSTEM to SourceSystemType.LIBRA.name,
        FIFO to "true",
      ),
    )
  }
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
