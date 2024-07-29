package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
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
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.*

const val CPR_COURT_EVENTS_TEMP_QUEUE_CONFIG_KEY = "cprcourtcaseeventstemporaryqueue"

@Component
@Profile(value = ["preprod", "test"])
class CourtEventFIFOTempListener(
  val objectMapper: ObjectMapper,
  val telemetryService: TelemetryService,
  var hmppsQueueService: HmppsQueueService,

) {
  @SqsListener(CPR_COURT_EVENTS_TEMP_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "hmpps-person-record-cpr_court_events_temporary_queue", kind = SERVER)
  fun onMessage(
    rawMessage: String,
  ) {
    // at the moment this will have both Common Platform and LIBRA messages, we should distinguish
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)

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
        TelemetryEventType.MESSAGE_RECEIVED_COURT,
        mapOf(
          SOURCE_SYSTEM to SourceSystemType.COMMON_PLATFORM.name,
          DEFENDANT_ID to it.id,
          EVENT_TYPE to sqsMessage.getMessageType(),
          MESSAGE_ID to sqsMessage.messageId,
          FIFO to "false",
        ),
      )
    }

    publishCourtMessage(sqsMessage.message, MessageType.COMMON_PLATFORM_HEARING) // republishing
  }

  internal fun publishCourtMessage(message: String, messageType: MessageType, topic: String = hmppsQueueService.findByTopicId("courteventstopicfifo")?.arn!!) {
    var messageBuilder = PublishRequest.builder()
      .topicArn(topic)
      .message(message)
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(messageType.name).build(),
          "messageId" to MessageAttributeValue.builder().dataType("String")
            .stringValue(UUID.randomUUID().toString()).build(),
        ),
      )
    if (topic.contains(".fifo")) {
      messageBuilder = messageBuilder.messageGroupId(UUID.randomUUID().toString())
    }

    hmppsQueueService.findByTopicId("courteventstopicfifo")?.snsClient?.publish(messageBuilder.build())?.get()
  }
}
