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
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.CourtMessagePublisher
import uk.gov.justice.digital.hmpps.personrecord.message.LARGE_CASE_EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED

@Component
class CourtEventProcessor(
  private val objectMapper: ObjectMapper,
  private val createUpdateService: CreateUpdateService,
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  @Value("\${publish-to-court-topic}")
  private val publishToCourtTopic: Boolean,
  private val courtMessagePublisher: CourtMessagePublisher,
  private val s3AsyncClient: S3AsyncClient,
) {

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
    if (publishToCourtTopic) {
      when (messageLargerThanThreshold(commonPlatformHearing)) {
        true -> courtMessagePublisher.publishLargeMessage(commonPlatformHearing, sqsMessage)
        else -> courtMessagePublisher.publishMessage(sqsMessage)
      }
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

  private fun isLargeMessage(sqsMessage: SQSMessage) = sqsMessage.getEventType() == LARGE_CASE_EVENT_TYPE

  private fun messageLargerThanThreshold(message: String): Boolean = message.toByteArray().size >= MAX_MESSAGE_SIZE

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
    if (publishToCourtTopic) {
      courtMessagePublisher.publishMessage(sqsMessage)
    }
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
