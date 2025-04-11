package uk.gov.justice.digital.hmpps.personrecord.message

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
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.publish
import java.util.concurrent.CompletableFuture

const val LARGE_CASE_EVENT_TYPE = "commonplatform.large.case.received"

@Component
class CourtMessagePublisher(
  private val personRepository: PersonRepository,
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

  private val snsExtendedClient = AmazonSNSExtendedAsyncClient(
    topic.snsClient,
    snsExtendedAsyncClientConfiguration,
  )

  fun publishLargeMessage(
    commonPlatformHearing: String,
    sqsMessage: SQSMessage,
    defendants: List<Defendant>?,
  ): CompletableFuture<PublishResponse> = runBlocking {
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

    val messageParser = JsonPath.parse(commonPlatformHearing)
    defendants?.forEach { defendant ->
      val person: PersonEntity? = defendant.id?.let { personRepository.findByDefendantId(it) }
      val defendantId = person?.defendantId
      val cprUUID = person?.personKey?.personId.toString()
      messageParser.put("$.hearing.prosecutionCases[?(@.defendants[?(@.id == '$defendantId')])].defendants[?(@.id == '$defendantId')]", "cprUUID", cprUUID)
    }

    val updatedSqsMessage = messageParser.jsonString()

    snsExtendedClient.publish(
      PublishRequest.builder().topicArn(topic.arn).messageAttributes(
        attributes,
      ).message(updatedSqsMessage)
        .build(),
    )
  }

  fun publishMessage(sqsMessage: SQSMessage, defendants: List<Defendant>?) {
    val attributes = mutableMapOf(
      "messageType" to MessageAttributeValue.builder()
        .dataType("String")
        .stringValue(sqsMessage.getMessageType())
        .build(),
    )

    sqsMessage.getHearingEventType()?.let {
      val hearingEventTypeValue =
        MessageAttributeValue.builder()
          .dataType("String")
          .stringValue(sqsMessage.getHearingEventType())
          .build()
      attributes.put("hearingEventType", hearingEventTypeValue)
    }

    // add the cprUUID
    val messageParser = JsonPath.parse(sqsMessage.message)
    defendants?.forEach { defendant ->
      val person: PersonEntity? = defendant.id?.let { personRepository.findByDefendantId(it) }
      val defendantId = person?.defendantId
      val cprUUID = person?.personKey?.personId.toString()
      messageParser.put("$.hearing.prosecutionCases[?(@.defendants[?(@.id == '$defendantId')])].defendants[?(@.id == '$defendantId')]", "cprUUID", cprUUID)
    }

    val updatedSqsMessage = messageParser.jsonString()

    topic.publish(
      eventType = sqsMessage.getEventType()!!,
      event = updatedSqsMessage,
      attributes = attributes,
    )
  }
}
