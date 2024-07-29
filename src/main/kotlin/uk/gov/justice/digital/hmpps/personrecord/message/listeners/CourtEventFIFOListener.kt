package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.instrumentation.annotations.WithSpan
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
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DEFENDANT_RECEIVED

const val CPR_COURT_EVENTS_FIFO_QUEUE_CONFIG_KEY = "cprcourteventsqueuefifo"

@Component
@Profile(value = ["preprod", "test"])
class CourtEventFIFOListener(
  val objectMapper: ObjectMapper,
  val telemetryService: TelemetryService,
) {
  @SqsListener(CPR_COURT_EVENTS_FIFO_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "hmpps-person-record-cpr_court_events_queue", kind = SERVER)
  fun onMessage(
    rawMessage: String,
  ) {
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

    val uniqueDefendants = commonPlatformHearingEvent.hearing.prosecutionCases
      .flatMap { it.defendants }
      .distinctBy {
        it.id
      }
    uniqueDefendants.forEach {
      telemetryService.trackEvent(
        DEFENDANT_RECEIVED,
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
      DEFENDANT_RECEIVED,
      mapOf(

        EVENT_TYPE to MessageType.LIBRA_COURT_CASE.name,
        MESSAGE_ID to sqsMessage.messageId,
        SOURCE_SYSTEM to SourceSystemType.LIBRA.name,
        FIFO to "true",
      ),
    )
  }
}
