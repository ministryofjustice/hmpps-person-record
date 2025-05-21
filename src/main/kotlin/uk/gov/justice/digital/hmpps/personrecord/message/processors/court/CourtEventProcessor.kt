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
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.queue.CourtMessagePublisher
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService

@Component
class CourtEventProcessor(
  private val objectMapper: ObjectMapper,
  private val createUpdateService: CreateUpdateService,
  private val personRepository: PersonRepository,
  @Value("\${publish-to-court-topic:false}")
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
      sqsMessage.isLargeMessage() -> runBlocking { getPayloadFromS3(sqsMessage) }
      else -> sqsMessage.message
    }

    val commonPlatformHearingEvent = objectMapper.readValue<CommonPlatformHearingEvent>(commonPlatformHearing)

    val defendants = commonPlatformHearingEvent.hearing.prosecutionCases
      .flatMap { it.defendants }
      .filterNot { it.isYouth }
      .distinctBy { it.id }
      .map { defendant -> Person.from(defendant) }
      .filter { it.isPerson() }
      .map {
        processCommonPlatformPerson(it)
      }

    if (publishToCourtTopic) {
      val updatedMessage = addCprUUIDToCommonPlatform(commonPlatformHearing, defendants)
      when (messageLargerThanThreshold(commonPlatformHearing)) {
        true -> courtMessagePublisher.publishLargeMessage(sqsMessage, updatedMessage)
        else -> courtMessagePublisher.publishMessage(sqsMessage, updatedMessage)
      }
    }
  }

  private fun processCommonPlatformPerson(person: Person): PersonEntity = createUpdateService.processPerson(person) {
    person.defendantId?.let {
      personRepository.findByDefendantId(it)
    }
  }

  private fun messageLargerThanThreshold(message: String): Boolean = message.toByteArray().size >= MAX_MESSAGE_SIZE

  private suspend fun getPayloadFromS3(sqsMessage: SQSMessage): String {
    val messageBody = objectMapper.readValue(sqsMessage.message, ArrayList::class.java)
    val (s3Key, s3BucketName) = objectMapper.readValue(objectMapper.writeValueAsString(messageBody[1]), LargeMessageBody::class.java)

    val request = GetObjectRequest.builder().key(s3Key).bucket(s3BucketName).build()
    return s3AsyncClient.getObject(
      request,
      AsyncResponseTransformer.toBytes(),
    ).join().asUtf8String()
  }

  private fun processLibraEvent(sqsMessage: SQSMessage) {
    val libraHearingEvent = objectMapper.readValue<LibraHearingEvent>(sqsMessage.message)
    val person = Person.from(libraHearingEvent)
    val personEntity = when {
      libraHearingEvent.isPerson() && person.isPerson() -> processLibraPerson(person)
      else -> null
    }
    if (publishToCourtTopic) {
      val updatedMessage = addCprUUIDToLibra(sqsMessage.message, personEntity)
      courtMessagePublisher.publishMessage(sqsMessage, updatedMessage)
    }
  }

  private fun processLibraPerson(person: Person): PersonEntity = createUpdateService.processPerson(person) {
    person.cId?.let {
      personRepository.findByCId(it)
    }
  }
}

data class LargeMessageBody(val s3Key: String, val s3BucketName: String)
