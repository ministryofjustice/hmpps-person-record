package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.sns.AmazonSNSExtendedAsyncClient
import software.amazon.sns.SNSExtendedAsyncClientConfiguration
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.publish

@Suppress("complexity:LongParameterList")
@Component
class CourtEventProcessor(
  private val objectMapper: ObjectMapper,
  private val createUpdateService: CreateUpdateService,
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val s3AsyncClient: S3AsyncClient,
  hmppsQueueService: HmppsQueueService,
  @Value("\${publish-to-court-topic}")
  private var publishToCourtTopic: Boolean,
  @Value("\${aws.cpr-court-message-bucket-name}") private val bucketName: String,
) {
  private val topic =
    hmppsQueueService.findByTopicId("cprcourtcasestopic")
      ?: throw MissingTopicException("Could not find topic ")

  companion object {
    const val MAX_MESSAGE_SIZE = 256 * 1024
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(sqsMessage: SQSMessage) {
    when (val messageType = sqsMessage.getMessageType()) {
      COMMON_PLATFORM_HEARING.name -> processCommonPlatformHearingEvent(sqsMessage)
      LIBRA_COURT_CASE.name -> processLibraEvent(sqsMessage)
      else -> {
        log.debug("Received case type $messageType")
      }
    }
  }

  private fun processCommonPlatformHearingEvent(sqsMessage: SQSMessage) {
    val commonPlatformHearing: String = when {
      isLargeMessage(sqsMessage) -> runBlocking { getPayloadFromS3(sqsMessage) }
      else -> sqsMessage.message
    }

    if (messageLargerThanThreshold(commonPlatformHearing)) {
      runBlocking {
        publishLargeMessage(commonPlatformHearing, sqsMessage)
      }
    } else {
      publishMessage(sqsMessage)
    }

    val commonPlatformHearingEvent = objectMapper.readValue<CommonPlatformHearingEvent>(commonPlatformHearing)

    val uniquePersonDefendants = commonPlatformHearingEvent.hearing.prosecutionCases
      .flatMap { it.defendants }
      .filterNot { it.isYouth }
      .filter { it.isPerson() }
      .distinctBy { it.id }

    uniquePersonDefendants.forEach { defendant ->
      processCommonPlatformPerson(defendant, sqsMessage)
    }
  }

  suspend fun publishLargeMessage(commonPlatformHearing: String, sqsMessage: SQSMessage) {
    if (publishToCourtTopic) {
      val snsExtendedAsyncClientConfiguration: SNSExtendedAsyncClientConfiguration =
        SNSExtendedAsyncClientConfiguration()
          .withPayloadSupportEnabled(s3AsyncClient, bucketName)

      val attributes = mutableMapOf(
        "messageType" to MessageAttributeValue.builder().dataType("String").stringValue("COMMON_PLATFORM_HEARING")
          .build(),
        "eventType" to MessageAttributeValue.builder().dataType("String")
          .stringValue("commonplatform.large.case.received").build(),
      )

      sqsMessage.getHearingEventType()?.let {
        // Only present on COMMON PLATFORM cases
        val hearingEventTypeValue =
          MessageAttributeValue.builder()
            .dataType("String")
            .stringValue(sqsMessage.getHearingEventType())
            .build()
        attributes.put("hearingEventType", hearingEventTypeValue)
      }

      val snsExtendedClient = AmazonSNSExtendedAsyncClient(
        topic.snsClient,
        snsExtendedAsyncClientConfiguration,
      )
      snsExtendedClient.publish(
        PublishRequest.builder().topicArn(topic.arn).messageAttributes(
          attributes,
        ).message(objectMapper.writeValueAsString(commonPlatformHearing))
          .build(),
      )
    }
  }

  private fun publishMessage(sqsMessage: SQSMessage) {
    if (publishToCourtTopic) {
      val attributes = mutableMapOf(
        "messageType" to MessageAttributeValue.builder()
          .dataType("String")
          .stringValue(sqsMessage.getMessageType())
          .build(),
      )

      sqsMessage.getHearingEventType()?.let {
        // Only present on COMMON PLATFORM cases
        val hearingEventTypeValue =
          MessageAttributeValue.builder()
            .dataType("String")
            .stringValue(sqsMessage.getHearingEventType())
            .build()
        attributes.put("hearingEventType", hearingEventTypeValue)
      }

      topic.publish(
        eventType = "commonplatform.case.received",
        event = objectMapper.writeValueAsString(sqsMessage.message),
        attributes = attributes,
      )
    }
  }

  private fun processCommonPlatformPerson(defendant: Defendant, sqsMessage: SQSMessage) {
    val person = Person.from(defendant)
    telemetryService.trackEvent(
      MESSAGE_RECEIVED,
      mapOf(
        EventKeys.DEFENDANT_ID to person.defendantId,
        EventKeys.EVENT_TYPE to COMMON_PLATFORM_HEARING.name,
        EventKeys.MESSAGE_ID to sqsMessage.messageId,
        EventKeys.SOURCE_SYSTEM to SourceSystemType.COMMON_PLATFORM.name,
      ),
    )
    createUpdateService.processPerson(person, null) {
      person.defendantId?.let {
        personRepository.findByDefendantId(it)
      }
    }
  }

  private fun isLargeMessage(sqsMessage: SQSMessage) = sqsMessage.getEventType() == "commonplatform.large.case.received"

  fun messageLargerThanThreshold(message: String): Boolean = message.toByteArray().size >= MAX_MESSAGE_SIZE

  private suspend fun getPayloadFromS3(sqsMessage: SQSMessage): String {
    val messageBody = objectMapper.readValue(sqsMessage.message, ArrayList::class.java)
    val message = objectMapper.readValue(objectMapper.writeValueAsString(messageBody[1]), LargeMessageBody::class.java)

    val request = GetObjectRequest.builder().key(message.s3Key).bucket(message.s3BucketName).build()
    return s3AsyncClient.getObject(
      request,
      AsyncResponseTransformer.toBytes(),
    ).join().asUtf8String()
  }

  private fun processLibraEvent(sqsMessage: SQSMessage) {
    publishMessage(sqsMessage)

    val libraHearingEvent = objectMapper.readValue<LibraHearingEvent>(sqsMessage.message)
    when {
      isLibraPerson(libraHearingEvent) -> processLibraPerson(libraHearingEvent, sqsMessage)
    }
  }

  private fun processLibraPerson(libraHearingEvent: LibraHearingEvent, sqsMessage: SQSMessage) {
    val person = Person.from(libraHearingEvent)
    telemetryService.trackEvent(
      MESSAGE_RECEIVED,
      mapOf(
        EventKeys.C_ID to person.cId,
        EventKeys.EVENT_TYPE to LIBRA_COURT_CASE.name,
        EventKeys.MESSAGE_ID to sqsMessage.messageId,
        EventKeys.SOURCE_SYSTEM to SourceSystemType.LIBRA.name,
      ),
    )
    createUpdateService.processPerson(person, null) {
      person.cId?.let {
        personRepository.findByCId(it)
      }
    }
  }

  private fun isLibraPerson(libraHearingEvent: LibraHearingEvent) = libraHearingEvent.defendantType == DefendantType.PERSON.value
}

data class LargeMessageBody(val s3Key: String, val s3BucketName: String)
