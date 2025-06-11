package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.LargeMessageBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.CourtMessagePublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class CommonPlatformEventProcessor(
  private val personRepository: PersonRepository,
  private val objectMapper: ObjectMapper,
  private val createUpdateService: CreateUpdateService,
  private val courtMessagePublisher: CourtMessagePublisher,
  private val s3AsyncClient: S3AsyncClient,
  private val publisher: ApplicationEventPublisher,
) {

  companion object {
    const val MAX_MESSAGE_SIZE = 256 * 1024
  }

  fun processEvent(sqsMessage: SQSMessage) {
    val commonPlatformHearing: String = when {
      sqsMessage.isLargeMessage() -> runBlocking { getPayloadFromS3(sqsMessage) }
      else -> sqsMessage.message
    }

    val commonPlatformHearingNode = objectMapper.readTree(commonPlatformHearing)

    commonPlatformHearingNode.path("hearing").path("prosecutionCases")
      .flatMap { it.path("defendants") }
      .filter {
        it.path("isYouth").isMissingNode or
          (
            it.path("croNumber").isMissingNode and
              it.path("pncId").isMissingNode
            )
      }
      .forEach {
        publisher.publishEvent(
          RecordTelemetry(
            TelemetryEventType.CPR_COMMON_PLATFORM_MISSING_KEYS,
            mapOf(
              EventKeys.DEFENDANT_ID to it.get("id").asText(),
            ),
          ),
        )
      }

    val commonPlatformHearingEvent = objectMapper.readValue<CommonPlatformHearingEvent>(commonPlatformHearing)

    val defendants = commonPlatformHearingEvent.hearing.prosecutionCases
      .asSequence()
      .flatMap { it.defendants }
      .filterNot { it.isYouth }
      .distinctBy { it.id }
      .map { defendant -> Person.from(defendant) }
      .filter { it.isPerson() }
      .map {
        processCommonPlatformPerson(it)
      }
      .toList()

    val updatedMessage = addCprUUIDToCommonPlatform(commonPlatformHearing, defendants)
    when (messageLargerThanThreshold(commonPlatformHearing)) {
      true -> courtMessagePublisher.publishLargeMessage(sqsMessage, updatedMessage)
      else -> courtMessagePublisher.publishMessage(sqsMessage, updatedMessage)
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

  private fun addCprUUIDToCommonPlatform(
    message: String,
    processedDefendants: List<PersonEntity>?,
  ): String {
    val messageParser = JsonPath.parse(message)
    processedDefendants?.forEach { defendant ->
      val defendantId = defendant.defendantId
      val cprUUID = defendant.personKey?.personUUID.toString()
      messageParser.put(
        "$.hearing.prosecutionCases[?(@.defendants[?(@.id == '$defendantId')])].defendants[?(@.id == '$defendantId')]",
        "cprUUID",
        cprUUID,
      )
    }
    return messageParser.jsonString()
  }
}
