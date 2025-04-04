package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.commonplatform

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttributes
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.message.LARGE_CASE_EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.message.processors.court.LargeMessageBody
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupContact
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.largeCommonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.largeCommonPlatformMessage
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomHearingId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.nio.charset.Charset
import java.util.UUID

class CommonPlatformCourtEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  lateinit var s3AsyncClient: S3AsyncClient

  @Value("\${aws.court-message-bucket-name}")
  lateinit var s3Bucket: String

  @Test
  fun `FIFO queue and topic remove duplicate messages`() {
    stubPersonMatchUpsert()
    stubNoMatchesPersonMatch()
    val pnc = randomPnc()
    val defendantId = randomDefendantId()
    val firstName = randomName()
    val lastName = randomName()
    val nationalInsuranceNumber = randomNationalInsuranceNumber()
    val hearingId = randomHearingId()
    blitz(30, 6) {
      publishCommonPlatformMessage(
        commonPlatformHearing(
          listOf(
            CommonPlatformHearingSetup(
              pnc = pnc,
              defendantId = defendantId,
              firstName = firstName,
              lastName = lastName,
              cro = "",
              nationalInsuranceNumber = nationalInsuranceNumber,
              hearingId = hearingId,
            ),
          ),
        ),
      )
    }

    expectNoMessagesOnQueueOrDlq(courtEventsQueue)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId),
    )
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf(
        "SOURCE_SYSTEM" to "COMMON_PLATFORM",
        "DEFENDANT_ID" to defendantId,
      ),
      0,
    )
  }

  @Test
  fun `should update an existing person record from common platform message`() {
    stubPersonMatchUpsert()
    val defendantId = randomDefendantId()
    val pnc = randomPnc()
    val cro = randomCro()
    val firstName = randomName()
    val lastName = randomName()

    val personKey = createPersonKey()
    val person = createPerson(
      Person(
        defendantId = defendantId,
        references = listOf(Reference(PNC, pnc), Reference(CRO, cro)),
        firstName = firstName,
        lastName = lastName,
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = personKey,
    )

    stubNoMatchesPersonMatch(matchId = person.matchId)

    val changedLastName = randomName()
    val messageId = publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(pnc = pnc, lastName = changedLastName, cro = cro, defendantId = defendantId))),
    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to COMMON_PLATFORM.name, "DEFENDANT_ID" to defendantId),
    )

    awaitAssert {
      val updatedPersonEntity = personRepository.findByDefendantId(defendantId)!!
      assertThat(updatedPersonEntity.lastName).isEqualTo(changedLastName)
      assertThat(updatedPersonEntity.references.getType(PNC).first().identifierValue).isEqualTo(pnc)
      assertThat(updatedPersonEntity.references.getType(CRO).first().identifierValue).isEqualTo(cro)
      assertThat(updatedPersonEntity.addresses.size).isEqualTo(1)
    }

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId),
    )
  }

  @Test
  fun `should create new people with additional fields from common platform message`() {
    stubPersonMatchScores()
    stubPersonMatchUpsert()
    val firstPnc = randomPnc()
    val firstName = randomName()
    val lastName = randomName()
    val secondPnc = randomPnc()
    val thirdPnc = randomPnc()

    val firstDefendantId = randomDefendantId()
    val secondDefendantId = randomDefendantId()
    val thirdDefendantId = randomDefendantId()

    val thirdDefendantNINumber = randomNationalInsuranceNumber()

    val buildingName = randomName()
    val buildingNumber = randomBuildingNumber()
    val thoroughfareName = randomName()
    val dependentLocality = randomName()
    val postTown = randomName()
    val postcode = randomPostcode()

    val messageId = publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            pnc = firstPnc,
            firstName = firstName,
            middleName = "mName1 mName2",
            lastName = lastName,
            defendantId = firstDefendantId,
            aliases = listOf(
              CommonPlatformHearingSetupAlias(firstName = "aliasFirstName1", lastName = "alisLastName1"),
              CommonPlatformHearingSetupAlias(firstName = "aliasFirstName2", lastName = "alisLastName2"),
            ),
          ),
          CommonPlatformHearingSetup(
            pnc = secondPnc,
            defendantId = secondDefendantId,
            contact = CommonPlatformHearingSetupContact(),
            address =
            CommonPlatformHearingSetupAddress(buildingName = buildingName, buildingNumber = buildingNumber, thoroughfareName = thoroughfareName, dependentLocality = dependentLocality, postTown = postTown, postcode = postcode),
          ),
          CommonPlatformHearingSetup(pnc = thirdPnc, defendantId = thirdDefendantId, nationalInsuranceNumber = thirdDefendantNINumber),
        ),
      ),

    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to firstDefendantId,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
      ),
    )
    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to secondDefendantId,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
      ),
    )
    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to thirdDefendantId,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
      ),
    )
    val firstPerson = awaitNotNullPerson {
      personRepository.findByDefendantId(firstDefendantId)
    }

    val secondPerson = awaitNotNullPerson {
      personRepository.findByDefendantId(secondDefendantId)
    }

    val thirdPerson = awaitNotNullPerson {
      personRepository.findByDefendantId(thirdDefendantId)
    }

    assertThat(firstPerson.references.getType(PNC).first().identifierValue).isEqualTo(firstPnc)
    assertThat(firstPerson.personKey).isNotNull()
    assertThat(firstPerson.masterDefendantId).isEqualTo(firstDefendantId)
    assertThat(firstPerson.firstName).isEqualTo(firstName)
    assertThat(firstPerson.middleNames).isEqualTo("mName1 mName2")
    assertThat(firstPerson.lastName).isEqualTo(lastName)
    assertThat(firstPerson.contacts).isEmpty()
    assertThat(firstPerson.addresses).isNotEmpty()
    assertThat(firstPerson.pseudonyms.size).isEqualTo(2)
    assertThat(firstPerson.pseudonyms[0].firstName).isEqualTo("aliasFirstName1")
    assertThat(firstPerson.pseudonyms[0].lastName).isEqualTo("alisLastName1")
    assertThat(firstPerson.pseudonyms[1].firstName).isEqualTo("aliasFirstName2")
    assertThat(firstPerson.pseudonyms[1].lastName).isEqualTo("alisLastName2")

    assertThat(secondPerson.pseudonyms).isEmpty()
    assertThat(secondPerson.addresses).isNotEmpty()
    assertThat(secondPerson.addresses[0].postcode).isEqualTo(postcode)
    assertThat(secondPerson.addresses[0].subBuildingName).isNull()
    assertThat(secondPerson.addresses[0].buildingName).isEqualTo(buildingName)
    assertThat(secondPerson.addresses[0].buildingNumber).isEqualTo(buildingNumber)
    assertThat(secondPerson.addresses[0].thoroughfareName).isEqualTo(thoroughfareName)
    assertThat(secondPerson.addresses[0].dependentLocality).isEqualTo(dependentLocality)
    assertThat(secondPerson.addresses[0].postTown).isEqualTo(postTown)
    assertThat(secondPerson.addresses[0].county).isNull()
    assertThat(secondPerson.addresses[0].country).isNull()
    assertThat(secondPerson.addresses[0].uprn).isNull()
    assertThat(secondPerson.references.getType(PNC).first().identifierValue).isEqualTo(secondPnc)
    assertThat(secondPerson.contacts.size).isEqualTo(3)
    assertThat(secondPerson.contacts[0].contactType).isEqualTo(HOME)
    assertThat(secondPerson.contacts[0].contactValue).isEqualTo("0207345678")
    assertThat(secondPerson.contacts[1].contactType).isEqualTo(MOBILE)
    assertThat(secondPerson.contacts[1].contactValue).isEqualTo("078590345677")
    assertThat(secondPerson.masterDefendantId).isEqualTo(secondDefendantId)

    assertThat(thirdPerson.pseudonyms).isEmpty()
    assertThat(thirdPerson.contacts.size).isEqualTo(0)
    assertThat(thirdPerson.references.getType(PNC).first().identifierValue).isEqualTo(thirdPnc)
    assertThat(thirdPerson.references.getType(NATIONAL_INSURANCE_NUMBER).first().identifierValue).isEqualTo(thirdDefendantNINumber)
    assertThat(thirdPerson.masterDefendantId).isEqualTo(thirdDefendantId)
  }

  @Test
  fun `should log Message Processing Failed telemetry event when an exception is thrown`() {
    val messageId = publishCommonPlatformMessage(
      "notAValidMessage",

    )

    checkTelemetry(
      MESSAGE_PROCESSING_FAILED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to COMMON_PLATFORM.name),
    )
  }

  @Test
  fun `should process large messages`() {
    stubPersonMatchScores()
    stubPersonMatchUpsert()
    val defendantId = randomDefendantId()

    val s3Key = UUID.randomUUID().toString()

    val request =
      PutObjectRequest.builder().bucket(s3Bucket).key(s3Key).build()

    runBlocking {
      s3AsyncClient.putObject(request, AsyncRequestBody.fromString(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId)))))
    }
    val messageId = publishLargeCommonPlatformMessage(
      largeCommonPlatformMessage(s3Key, s3Bucket),
    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to COMMON_PLATFORM.name, "EVENT_TYPE" to COMMON_PLATFORM_HEARING.name),
    )
    awaitNotNullPerson { personRepository.findByDefendantId(defendantId) }
  }

  @Test
  fun `should process messages with pnc as empty string and null`() {
    stubPersonMatchScores()
    stubPersonMatchUpsert()
    val firstDefendantId = randomDefendantId()
    val secondDefendantId = randomDefendantId()

    val messageId = publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(pnc = "", defendantId = firstDefendantId),
          CommonPlatformHearingSetup(pnc = null, defendantId = secondDefendantId),
        ),
      ),

    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to COMMON_PLATFORM.name, "EVENT_TYPE" to COMMON_PLATFORM_HEARING.name),
      times = 2,
    )
    val personWithEmptyPnc = awaitNotNullPerson {
      personRepository.findByDefendantId(firstDefendantId)
    }
    assertThat(personWithEmptyPnc.references.getType(PNC)).isEqualTo(emptyList<ReferenceEntity>())

    val personWithNullPnc = personRepository.findByDefendantId(secondDefendantId)
    assertThat(personWithNullPnc?.references?.getType(PNC)).isEqualTo(emptyList<ReferenceEntity>())
  }

  @Test
  fun `should not process youth cases`() {
    val youthDefendantId = randomDefendantId()
    val messageId = publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = youthDefendantId, isYouth = true),
        ),
      ),

    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to COMMON_PLATFORM.name, "EVENT_TYPE" to COMMON_PLATFORM_HEARING.name),
      times = 0,
    )
  }

  @Test
  fun `should process when is youth is null`() {
    stubPersonMatchScores()
    stubPersonMatchUpsert()
    val defendantId = randomDefendantId()
    val messageId = publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = defendantId, isYouth = null),
        ),
      ),

    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "EVENT_TYPE" to COMMON_PLATFORM_HEARING.name,
        "DEFENDANT_ID" to defendantId,
      ),
    )
  }

  @Test
  fun `should not process when is organisation`() {
    val defendantId = randomDefendantId()
    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = defendantId, isPerson = false),
        ),
      ),
    )

    awaitAssert { assertThat(personRepository.findByDefendantId(defendantId)).isNull() }
  }

  @Test
  fun `should publish incoming event to court topic`() {
    val defendantId = randomDefendantId()
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId))),
    )

    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }
    assertThat(sqsMessage?.messageAttributes?.eventType).isEqualTo(MessageAttribute("commonplatform.case.received"))
    val commonPlatformHearing: String = sqsMessage?.message!!

    val commonPlatformHearingAttributes: MessageAttributes? = sqsMessage.messageAttributes

    assertThat(commonPlatformHearing.contains(defendantId)).isEqualTo(true)

    assertThat(commonPlatformHearingAttributes?.messageType?.value).isEqualTo(COMMON_PLATFORM_HEARING.name)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId),
    )
  }

  @Test
  fun `should publish incoming large message to CPR court topic`() {
    val defendantId = randomDefendantId()
    stubPersonMatchUpsert()
    stubPersonMatchScores()
    val s3Key = UUID.randomUUID().toString()

    val incomingMessageFromS3 =
      largeCommonPlatformHearing(defendantId).toByteArray(Charset.forName("UTF8"))

    assertThat(incomingMessageFromS3.size).isGreaterThan(256 * 1024)

    val putObjectRequest = PutObjectRequest.builder().bucket(s3Bucket).key(s3Key).build()

    runBlocking {
      s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromBytes(incomingMessageFromS3))
    }

    publishLargeCommonPlatformMessage(
      largeCommonPlatformMessage(s3Key, s3Bucket),
    )

    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }

    val messageBody = objectMapper.readValue(sqsMessage?.message, ArrayList::class.java)
    val message = objectMapper.readValue(objectMapper.writeValueAsString(messageBody[1]), LargeMessageBody::class.java)
    assertThat(sqsMessage?.messageAttributes?.eventType).isEqualTo(MessageAttribute(LARGE_CASE_EVENT_TYPE))
    assertThat(sqsMessage?.messageAttributes?.hearingEventType).isEqualTo(MessageAttribute("ConfirmedOrUpdated"))
    val getRequest =
      GetObjectRequest.builder().key(message.s3Key).bucket(message.s3BucketName).build()

    val body = runBlocking {
      s3AsyncClient.getObject(
        getRequest,
        AsyncResponseTransformer.toBytes(),
      ).join().asUtf8String()
    }
    assertThat(body.contains(defendantId)).isEqualTo(true)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId),
      timeout = 10L,
    )
  }
}
