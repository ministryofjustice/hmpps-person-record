package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.LargeMessageBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.service.queue.CourtMessagePublisher

@Component
class CommonPlatformEventProcessor(
  private val personRepository: PersonRepository,
  private val objectMapper: ObjectMapper,
  private val transactionalCommonPlatformProcessor: TransactionalCommonPlatformProcessor,
  private val courtMessagePublisher: CourtMessagePublisher,
  private val s3AsyncClient: S3AsyncClient,
) {

  companion object {
    const val MAX_MESSAGE_SIZE = 256 * 1024
  }

  fun processEvent(sqsMessage: SQSMessage) {
    val commonPlatformHearing: String = when {
      sqsMessage.isLargeMessage() -> runBlocking { getPayloadFromS3(sqsMessage) }
      else -> sqsMessage.message
    }

    val commonPlatformHearingEvent = objectMapper.readValue<CommonPlatformHearingEvent>(commonPlatformHearing)

    val defendants = commonPlatformHearingEvent.hearing.prosecutionCases
      .asSequence()
      .flatMap { it.defendants }
      .filterNot { it.isYouth }
      .distinctBy { it.id }
      .map { populateIdentifiersFromDefendantWhenMissing(it) }
      .map { Person.from(it) }
      .filter { it.isPerson() }
      .map {
        transactionalCommonPlatformProcessor.processCommonPlatformPerson(it)
      }
      .toList()

    val updatedMessage = addCprUUIDToCommonPlatform(commonPlatformHearing, defendants)
    when (messageLargerThanThreshold(commonPlatformHearing)) {
      true -> courtMessagePublisher.publishLargeMessage(sqsMessage, updatedMessage)
      else -> courtMessagePublisher.publishMessage(sqsMessage, updatedMessage)
    }
  }

  private fun messageLargerThanThreshold(message: String): Boolean = message.toByteArray().size >= MAX_MESSAGE_SIZE

  private suspend fun getPayloadFromS3(sqsMessage: SQSMessage): String {
    val messageBody = objectMapper.readValue(sqsMessage.message, ArrayList::class.java)
    val (s3Key, s3BucketName) = objectMapper.readValue(
      objectMapper.writeValueAsString(messageBody[1]),
      LargeMessageBody::class.java,
    )

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

  private fun populateIdentifiersFromDefendantWhenMissing(defendant: Defendant): Defendant {
    if (defendant.isPncMissing && defendant.isCroMissing) {
      defendant.id?.let { personRepository.findByDefendantId(it) }?.let { existingDefendant ->

        defendant.pncId =
          PNCIdentifier.from(existingDefendant.references.firstOrNull { it.identifierType == IdentifierType.PNC }?.identifierValue)
        defendant.cro =
          CROIdentifier.from(existingDefendant.references.firstOrNull { it.identifierType == IdentifierType.CRO }?.identifierValue)
      }
    }
    return defendant
  }
}
