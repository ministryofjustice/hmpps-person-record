package uk.gov.justice.digital.hmpps.personrecord.service.queue

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.sns.AmazonSNSExtendedAsyncClient
import software.amazon.sns.SNSExtendedAsyncClientConfiguration
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.publish
import java.util.concurrent.CompletableFuture

const val LARGE_CASE_EVENT_TYPE = "commonplatform.large.case.received"

@Component
class CourtMessagePublisher(
  s3AsyncClient: S3AsyncClient,
  hmppsQueueService: HmppsQueueService,
  @Value("\${aws.cpr-court-message-bucket-name}") private val bucketName: String,
) {
  private val topic =
    hmppsQueueService.findByTopicId("cprcourtcasestopic")
      ?: throw MissingTopicException("Could not find topic ")

  private val snsExtendedAsyncClientConfiguration: SNSExtendedAsyncClientConfiguration =
    SNSExtendedAsyncClientConfiguration()
      .withPayloadSupportEnabled(s3AsyncClient, bucketName)
      .withAlwaysThroughS3(true)

  private val snsExtendedClient = AmazonSNSExtendedAsyncClient(
    topic.snsClient,
    snsExtendedAsyncClientConfiguration,
  )

  fun publishLargeMessage(
    sqsMessage: SQSMessage,
    updatedMessage: String,
  ): CompletableFuture<PublishResponse> = runBlocking {
    val attributes = mutableMapOf(
      "messageType" to MessageAttributeValue.builder().dataType("String")
        .stringValue(sqsMessage.getMessageType()).build(),
      "eventType" to MessageAttributeValue.builder().dataType("String")
        .stringValue(LARGE_CASE_EVENT_TYPE).build(), // to enum
    )

    attributes.addHearingEventType(sqsMessage)

    snsExtendedClient.publish(
      PublishRequest.builder().topicArn(topic.arn).messageAttributes(
        attributes,
      ).message(updatedMessage)
        .build(),
    )
  }

  fun publishMessage(
    sqsMessage: SQSMessage,
    updatedMessage: String,
  ) {
    val attributes = mutableMapOf(
      "messageType" to MessageAttributeValue.builder()
        .dataType("String")
        .stringValue(sqsMessage.getMessageType())
        .build(),
    )

    attributes.addHearingEventType(sqsMessage)

    topic.publish(
      eventType = sqsMessage.getEventType()!!,
      event = updatedMessage,
      attributes = attributes,
    )
  }

  private fun MutableMap<String, MessageAttributeValue>.addHearingEventType(
    sqsMessage: SQSMessage,
  ) {
    sqsMessage.getHearingEventType()?.let {
      val hearingEventTypeValue =
        MessageAttributeValue.builder()
          .dataType("String")
          .stringValue(it)
          .build()
      this.put("hearingEventType", hearingEventTypeValue)
    }
  }
}
