package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.LargeMessageBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.extensions.getCROs
import uk.gov.justice.digital.hmpps.personrecord.extensions.getPNCs
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.service.queue.CourtMessagePublisher

@Component
class CommonPlatformEventProcessor(
  private val personRepository: PersonRepository,
  private val jsonMapper: JsonMapper,
  private val transactionalCommonPlatformProcessor: TransactionalCommonPlatformProcessor,
  private val courtMessagePublisher: CourtMessagePublisher,
  private val s3AsyncClient: S3AsyncClient,
) {

  companion object {
    const val MAX_MESSAGE_SIZE = 245 * 1024
  }

  fun processEvent(sqsMessage: SQSMessage) {
    val commonPlatformHearing: String = when {
      sqsMessage.isLargeMessage() -> runBlocking { getPayloadFromS3(sqsMessage) }
      else -> sqsMessage.message
    }

    val commonPlatformHearingEvent = jsonMapper.readValue<CommonPlatformHearingEvent>(commonPlatformHearing)

    val defendants = commonPlatformHearingEvent.hearing.prosecutionCases
      .asSequence()
      .flatMap { it.defendants }
      .filterNot { it.isYouth ?: false }
      .distinctBy { it.id }
      .map { populateIdentifiersFromDefendantWhenMissing(it) }
      .map { Person.from(it) }
      .filter { it.isPerson() }
      .map { keepFormerAddresses(it) }
      .map {
        transactionalCommonPlatformProcessor.processCommonPlatformPerson(it)
      }
      .toList()

    val updatedMessage = addCprUUIDToCommonPlatform(commonPlatformHearing, defendants)
    when (messageLargerThanThreshold(updatedMessage)) {
      true -> courtMessagePublisher.publishLargeMessage(sqsMessage, updatedMessage)
      else -> courtMessagePublisher.publishMessage(sqsMessage, updatedMessage)
    }
  }

  private fun messageLargerThanThreshold(message: String): Boolean = message.toByteArray().size >= MAX_MESSAGE_SIZE

  private suspend fun getPayloadFromS3(sqsMessage: SQSMessage): String {
    val messageBody = jsonMapper.readValue<ArrayList<Any>>(sqsMessage.message)
    val (s3Key, s3BucketName) = jsonMapper.readValue<LargeMessageBody>(
      jsonMapper.writeValueAsString(messageBody[1]),
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
    if (defendant.isPncMissing || defendant.isCroMissing) {
      defendant.id?.let { personRepository.findByDefendantId(it) }?.let { existingDefendant ->
        defendant.retainPncOrCro(existingDefendant)
      }
    }
    return defendant
  }

  private fun Defendant.retainPncOrCro(personEntity: PersonEntity) {
    when {
      this.isPncMissing ->
        this.pncId =
          PNCIdentifier.from(personEntity.references.getPNCs().firstOrNull())
      this.isCroMissing ->
        this.cro =
          CROIdentifier.from(personEntity.references.getCROs().firstOrNull())
    }
  }

  private fun keepFormerAddresses(person: Person): Person {
    val mainAddress = person.addresses.firstOrNull()
    if (mainAddress == null) {
      person.addresses = personRepository.findByDefendantId(person.defendantId!!)?.addresses?.map { Address.from(it) } ?: emptyList()
      return person
    }
    val existingAddressesDemoted = personRepository.findByDefendantId(person.defendantId!!)?.addresses?.let {
      setAddressesToPrevious(mainAddress, it)
    } ?: emptyList()

    person.addresses = existingAddressesDemoted + mainAddress
    return person
  }

  private fun setAddressesToPrevious(mainAddress: Address, existingAddressEntities: List<AddressEntity>): List<Address> {
    val existingAddresses = existingAddressEntities
      .map { Address.from(it) }
      .filter { it.copy(statusCode = null) != mainAddress.copy(statusCode = null) }
    existingAddresses.forEach { it.statusCode = AddressStatusCode.P }
    return existingAddresses
  }
}
