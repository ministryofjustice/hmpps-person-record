package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.DEFENDANT_ID
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.FIFO
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.MESSAGE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DEFENDANT_RECEIVED
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.util.UUID

const val CPR_COURT_EVENTS_TEMP_QUEUE_CONFIG_KEY = "cprcourtcaseeventstemporaryqueue"

@Component
@Profile(value = ["preprod", "test"])
class CourtEventTempListener(
  val objectMapper: ObjectMapper,
  val telemetryService: TelemetryService,
  val hmppsQueueService: HmppsQueueService,

) {

  @SqsListener(CPR_COURT_EVENTS_TEMP_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(
    rawMessage: String,
  ) {
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)

    when (sqsMessage.getMessageType()) {
      COMMON_PLATFORM_HEARING.name -> processCommonPlatformHearingEvent(sqsMessage)
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
          SOURCE_SYSTEM to COMMON_PLATFORM.name,
          DEFENDANT_ID to it.id,
          EVENT_TYPE to sqsMessage.getMessageType(),
          MESSAGE_ID to sqsMessage.messageId,
          FIFO to "false",
        ),
      )
    }

    republishCourtMessage(objectMapper.writeValueAsString(commonPlatformHearingEvent), COMMON_PLATFORM_HEARING)
  }

  private fun processLibraEvent(sqsMessage: SQSMessage) {
    telemetryService.trackEvent(
      DEFENDANT_RECEIVED,
      mapOf(

        EVENT_TYPE to LIBRA_COURT_CASE.name,
        MESSAGE_ID to sqsMessage.messageId,
        SOURCE_SYSTEM to SourceSystemType.LIBRA.name,
        FIFO to "false",
      ),
    )
    republishCourtMessage(sqsMessage.message, LIBRA_COURT_CASE)
  }

  private fun republishCourtMessage(message: String, messageType: MessageType) {
    val topic = hmppsQueueService.findByTopicId("courteventsfifotopic")
      ?: throw MissingTopicException("Could not find topic courteventsfifotopic")
    val messageBuilder = PublishRequest.builder()
      .topicArn(topic.arn)
      .message(message)
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(messageType.name).build(),
          "messageId" to MessageAttributeValue.builder().dataType("String")
            .stringValue(UUID.randomUUID().toString()).build(),
        ),
      ).messageGroupId(UUID.randomUUID().toString())

    topic.snsClient.publish(messageBuilder.build())?.get()
  }
}
