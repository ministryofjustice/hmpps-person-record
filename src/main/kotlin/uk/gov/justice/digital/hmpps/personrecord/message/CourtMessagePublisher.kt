package uk.gov.justice.digital.hmpps.personrecord.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.publish
import java.util.concurrent.CompletableFuture

const val LARGE_CASE_EVENT_TYPE = "commonplatform.large.case.received"

@Component
class CourtMessagePublisher(
  s3AsyncClient: S3AsyncClient,
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  @Value("\${aws.cpr-court-message-bucket-name}") private val bucketName: String,
) {
  private val topic =
    hmppsQueueService.findByTopicId("cprcourtcasestopic")
      ?: throw MissingTopicException("Could not find topic ")

  private val snsExtendedAsyncClientConfiguration: SNSExtendedAsyncClientConfiguration =
    SNSExtendedAsyncClientConfiguration()
      .withPayloadSupportEnabled(s3AsyncClient, bucketName)

  private val snsExtendedClient = AmazonSNSExtendedAsyncClient(
    topic.snsClient,
    snsExtendedAsyncClientConfiguration,
  )

  fun publishLargeMessage(commonPlatformHearing: String, sqsMessage: SQSMessage): CompletableFuture<PublishResponse> = runBlocking {
    val attributes = mutableMapOf(
      "messageType" to MessageAttributeValue.builder().dataType("String").stringValue(sqsMessage.getMessageType())
        .build(),
      "eventType" to MessageAttributeValue.builder().dataType("String")
        .stringValue(LARGE_CASE_EVENT_TYPE).build(), // to enum
    )

    sqsMessage.getHearingEventType()?.let {
      val hearingEventTypeValue =
        MessageAttributeValue.builder()
          .dataType("String")
          .stringValue(sqsMessage.getHearingEventType())
          .build()
      attributes.put("hearingEventType", hearingEventTypeValue)
    }

    snsExtendedClient.publish(
      PublishRequest.builder().topicArn(topic.arn).messageAttributes(
        attributes,
      ).message(objectMapper.writeValueAsString(commonPlatformHearing))
        .build(),
    )
  }

  fun publishMessage(sqsMessage: SQSMessage, processedDefendants: List<PersonEntity>? = null) {
    val attributes = mutableMapOf(
      "messageType" to MessageAttributeValue.builder()
        .dataType("String")
        .stringValue(sqsMessage.getMessageType())
        .build(),
    )
    println(processedDefendants?.size)
    sqsMessage.getHearingEventType()?.let {
      val hearingEventTypeValue =
        MessageAttributeValue.builder()
          .dataType("String")
          .stringValue(sqsMessage.getHearingEventType())
          .build()
      attributes.put("hearingEventType", hearingEventTypeValue)
    }
    val messageParser = JsonPath.parse(sqsMessage.message)

    processedDefendants?.forEach { defendant ->
      val defendantId = defendant.defendantId
      val cprUUID = defendant.personKey?.personId.toString()
      messageParser.put("$.hearing.prosecutionCases[?(@.defendants[?(@.id == '$defendantId')])].defendants[?(@.id == '$defendantId')]", "cprUUID", cprUUID)
    }

    topic.publish(
      eventType = sqsMessage.getEventType()!!,
      event = messageParser.jsonString(),
      attributes = attributes,
    )
  }
}
