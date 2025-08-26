package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.commonplatform

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
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
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.LargeMessageBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttributes
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.queue.LARGE_CASE_EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupContact
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.largeCommonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.largeCommonPlatformMessage
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.nio.charset.Charset
import java.time.LocalDateTime.now
import java.util.UUID

class CommonPlatformCourtEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  lateinit var s3AsyncClient: S3AsyncClient

  @Value("\${aws.court-message-bucket-name}")
  lateinit var s3Bucket: String

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
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(pnc = pnc, lastName = changedLastName, cro = cro, defendantId = defendantId))),
    )

    awaitAssert {
      val updatedPersonEntity = personRepository.findByDefendantId(defendantId)!!
      assertThat(updatedPersonEntity.getPrimaryName().lastName).isEqualTo(changedLastName)
      assertThat(updatedPersonEntity.getPnc()).isEqualTo(pnc)
      assertThat(updatedPersonEntity.getCro()).isEqualTo(cro)
      assertThat(updatedPersonEntity.addresses.size).isEqualTo(1)
    }

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId),
    )
  }

  @Test
  fun `should update an existing person record from common platform message with no primary name in pseudonym table`() {
    stubPersonMatchUpsert()
    val defendantId = randomDefendantId()
    val pnc = randomPnc()
    val cro = randomCro()
    val personKey = createPersonKey()
    val ethnicity = randomCommonPlatformEthnicity()

    val person = Person(
      defendantId = defendantId,
      references = listOf(Reference(PNC, pnc), Reference(CRO, cro)),
      sourceSystem = COMMON_PLATFORM,
    )

    val personEntity = PersonEntity(
      defendantId = person.defendantId,
      crn = person.crn,
      prisonNumber = person.prisonNumber,
      masterDefendantId = person.masterDefendantId,
      sourceSystem = person.sourceSystem,
      ethnicity = person.ethnicity,
      religion = person.religion,
      matchId = UUID.randomUUID(),
      cId = person.cId,
      lastModified = now(),
      sexCode = person.sexCode,
    )
    personEntity.personKey = personKey
    personKeyRepository.saveAndFlush(personKey)
    personRepository.saveAndFlush(personEntity)
    stubNoMatchesPersonMatch(matchId = personEntity.matchId)

    val changedLastName = randomName()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(pnc = pnc, lastName = changedLastName, cro = cro, defendantId = defendantId, ethnicity = CommonPlatformHearingSetupEthnicity(ethnicity)))),
    )

    awaitAssert {
      val updatedPersonEntity = personRepository.findByDefendantId(defendantId)!!
      val storedEthnicity = ethnicityCodeRepository.findByCode(ethnicity)
      assertThat(updatedPersonEntity.getPrimaryName().lastName).isEqualTo(changedLastName)
      assertThat(updatedPersonEntity.getPnc()).isEqualTo(pnc)
      assertThat(updatedPersonEntity.getCro()).isEqualTo(cro)
      assertThat(updatedPersonEntity.addresses.size).isEqualTo(1)
      assertThat(updatedPersonEntity.ethnicityCode?.code).isEqualTo(storedEthnicity?.code)
      assertThat(updatedPersonEntity.ethnicityCode?.description).isEqualTo(storedEthnicity?.description)
    }

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId),
    )
  }

  @Test
  fun `should create new people from common platform message`() {
    stubPersonMatchScores()
    stubPersonMatchUpsert()
    val firstPnc = randomPnc()
    val firstName = randomName()
    val lastName = randomName()
    val secondPnc = randomPnc()
    val thirdPnc = randomPnc()
    val fourthPnc = randomPnc()

    val firstDefendantId = randomDefendantId()
    val secondDefendantId = randomDefendantId()
    val thirdDefendantId = randomDefendantId()
    val fourthDefendantId = randomDefendantId()

    val thirdDefendantNINumber = randomNationalInsuranceNumber()
    val fourthDefendantNINumber = randomNationalInsuranceNumber()

    val buildingName = randomName()
    val buildingNumber = randomBuildingNumber()
    val thoroughfareName = randomName()
    val dependentLocality = randomName()
    val postTown = randomName()
    val postcode = randomPostcode()
    val title = "Mr"
    val ethnicity = randomCommonPlatformEthnicity()

    val firstNationality = randomCommonPlatformNationalityCode()
    val secondNationality = randomCommonPlatformNationalityCode()

    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            gender = "MALE",
            pnc = firstPnc,
            title = title,
            firstName = firstName,
            middleName = "mName1 mName2",
            lastName = lastName,
            defendantId = firstDefendantId,
            aliases = listOf(
              CommonPlatformHearingSetupAlias(firstName = "aliasFirstName1", lastName = "aliasLastName1"),
              CommonPlatformHearingSetupAlias(firstName = "aliasFirstName2", lastName = "aliasLastName2"),
            ),
            nationalityCode = firstNationality,
            ethnicity = CommonPlatformHearingSetupEthnicity(ethnicity),
          ),
          CommonPlatformHearingSetup(
            gender = "FEMALE",
            pnc = secondPnc,
            defendantId = secondDefendantId,
            contact = CommonPlatformHearingSetupContact(),
            nationalityCode = secondNationality,
            address =
            CommonPlatformHearingSetupAddress(buildingName = buildingName, buildingNumber = buildingNumber, thoroughfareName = thoroughfareName, dependentLocality = dependentLocality, postTown = postTown, postcode = postcode),
          ),
          CommonPlatformHearingSetup(pnc = thirdPnc, defendantId = thirdDefendantId, nationalInsuranceNumber = thirdDefendantNINumber, gender = "NOT SPECIFIED"),
          CommonPlatformHearingSetup(pnc = fourthPnc, defendantId = fourthDefendantId, nationalInsuranceNumber = fourthDefendantNINumber, gender = "UNSUPPORTED GENDER CODE"),
        ),
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

    val forthPerson = awaitNotNullPerson {
      personRepository.findByDefendantId(fourthDefendantId)
    }

    assertThat(firstPerson.getPnc()).isEqualTo(firstPnc)
    assertThat(firstPerson.personKey).isNotNull()
    assertThat(firstPerson.masterDefendantId).isEqualTo(firstDefendantId)
    assertThat(firstPerson.getPrimaryName().titleCode?.code).isEqualTo("MR")
    assertThat(firstPerson.getPrimaryName().titleCode?.description).isEqualTo("Mr")
    assertThat(firstPerson.getPrimaryName().firstName).isEqualTo(firstName)
    assertThat(firstPerson.getPrimaryName().middleNames).isEqualTo("mName1 mName2")
    assertThat(firstPerson.getPrimaryName().lastName).isEqualTo(lastName)
    assertThat(firstPerson.contacts).isEmpty()
    assertThat(firstPerson.addresses).isNotEmpty()
    assertThat(firstPerson.nationalities.size).isEqualTo(1)
    assertThat(firstPerson.nationalities.first().nationalityCode?.code).isEqualTo(firstNationality.getNationalityCodeEntityFromCommonPlatformCode()?.code)
    assertThat(firstPerson.nationalities.first().nationalityCode?.description).isEqualTo(firstNationality.getNationalityCodeEntityFromCommonPlatformCode()?.description)
    assertThat(firstPerson.getAliases().size).isEqualTo(2)
    assertThat(firstPerson.getAliases()[0].titleCode).isNull()
    assertThat(firstPerson.getAliases()[0].firstName).isEqualTo("aliasFirstName1")
    assertThat(firstPerson.getAliases()[0].lastName).isEqualTo("aliasLastName1")
    assertThat(firstPerson.getAliases()[1].titleCode).isNull()
    assertThat(firstPerson.getAliases()[1].firstName).isEqualTo("aliasFirstName2")
    assertThat(firstPerson.getAliases()[1].lastName).isEqualTo("aliasLastName2")
    assertThat(firstPerson.sexCode).isEqualTo(SexCode.M)
    val ethnicityCode = ethnicityCodeRepository.findByCode(ethnicity)
    assertThat(firstPerson.ethnicityCode?.code).isEqualTo(ethnicityCode?.code)
    assertThat(firstPerson.ethnicityCode?.description).isEqualTo(ethnicityCode?.description)

    assertThat(secondPerson.getAliases()).isEmpty()
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
    assertThat(secondPerson.getPnc()).isEqualTo(secondPnc)
    assertThat(secondPerson.contacts.size).isEqualTo(3)
    assertThat(secondPerson.contacts[0].contactType).isEqualTo(HOME)
    assertThat(secondPerson.contacts[0].contactValue).isEqualTo("0207345678")
    assertThat(secondPerson.contacts[1].contactType).isEqualTo(MOBILE)
    assertThat(secondPerson.contacts[1].contactValue).isEqualTo("078590345677")
    assertThat(secondPerson.masterDefendantId).isEqualTo(secondDefendantId)
    assertThat(secondPerson.sexCode).isEqualTo(SexCode.F)
    assertThat(secondPerson.nationalities.size).isEqualTo(1)
    assertThat(secondPerson.nationalities.first().nationalityCode?.code).isEqualTo(secondNationality.getNationalityCodeEntityFromCommonPlatformCode()?.code)
    assertThat(secondPerson.nationalities.first().nationalityCode?.description).isEqualTo(secondNationality.getNationalityCodeEntityFromCommonPlatformCode()?.description)

    assertThat(thirdPerson.getAliases()).isEmpty()
    assertThat(thirdPerson.contacts.size).isEqualTo(0)
    assertThat(thirdPerson.getPnc()).isEqualTo(thirdPnc)
    assertThat(thirdPerson.references.getType(NATIONAL_INSURANCE_NUMBER).first().identifierValue).isEqualTo(thirdDefendantNINumber)
    assertThat(thirdPerson.masterDefendantId).isEqualTo(thirdDefendantId)
    assertThat(thirdPerson.sexCode).isEqualTo(SexCode.NS)

    assertThat(forthPerson.getAliases()).isEmpty()
    assertThat(forthPerson.contacts.size).isEqualTo(0)
    assertThat(forthPerson.getPnc()).isEqualTo(fourthPnc)
    assertThat(forthPerson.references.getType(NATIONAL_INSURANCE_NUMBER).first().identifierValue).isEqualTo(fourthDefendantNINumber)
    assertThat(forthPerson.masterDefendantId).isEqualTo(fourthDefendantId)
    assertThat(forthPerson.sexCode).isEqualTo(SexCode.N)
  }

  @Test
  fun `should put message on dlq when an exception is thrown`() {
    publishCommonPlatformMessage("notAValidMessage")

    expectOneMessageOnDlq(courtEventsQueue)
  }

  @Test
  fun `should process messages with pnc as empty string and null`() {
    stubPersonMatchScores()
    stubPersonMatchUpsert()
    val firstDefendantId = randomDefendantId()
    val secondDefendantId = randomDefendantId()

    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(pnc = "", defendantId = firstDefendantId),
          CommonPlatformHearingSetup(pnc = null, defendantId = secondDefendantId),
        ),
      ),

    )

    val personWithEmptyPnc = awaitNotNullPerson {
      personRepository.findByDefendantId(firstDefendantId)
    }
    assertThat(personWithEmptyPnc.references.getType(PNC)).isEqualTo(emptyList<ReferenceEntity>())

    val personWithNullPnc = personRepository.findByDefendantId(secondDefendantId)
    assertThat(personWithNullPnc?.references?.getType(PNC)).isEqualTo(emptyList<ReferenceEntity>())
  }

  @Test
  fun `should not join new common platform person with a cluster which is below join threshold`() {
    val defendantId = randomDefendantId()
    val existingPerson = createPersonWithNewKey(createRandomCommonPlatformPersonDetails(randomDefendantId()))

    stubPersonMatchUpsert()
    stubOnePersonMatchAboveFractureThreshold(matchedRecord = existingPerson.matchId)

    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = defendantId),
        ),
      ),
    )

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId))
    checkEventLogExist(defendantId, CPRLogEvents.CPR_RECORD_CREATED)
    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId))

    awaitNotNullPerson { personRepository.findByDefendantId(defendantId) }
  }

  @Test
  fun `should republish youth cases without saving or adding a UUID`() {
    val youthDefendantId = randomDefendantId()
    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = youthDefendantId, isYouth = true),
        ),
      ),
    )

    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute("commonplatform.case.received"))
    assertThat(sqsMessage.message.contains("cprUUID")).isFalse()
    assertThat(sqsMessage.message.contains(youthDefendantId)).isTrue()
    assertThat(personRepository.findByDefendantId(youthDefendantId)).isNull()
  }

  @Test
  fun `should process when is youth is null`() {
    stubPersonMatchScores()
    stubPersonMatchUpsert()
    val defendantId = randomDefendantId()
    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = defendantId, isYouth = null),
        ),
      ),
    )

    checkTelemetry(CPR_RECORD_CREATED, mapOf("DEFENDANT_ID" to defendantId))
  }

  @Test
  fun `should republish message and not save when organisation`() {
    val defendantId = randomDefendantId()
    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = defendantId, isPerson = false),
        ),
      ),
    )

    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute("commonplatform.case.received"))
    assertThat(sqsMessage.message.contains("cprUUID")).isFalse()
    assertThat(sqsMessage.message.contains(defendantId)).isTrue()

    assertThat(personRepository.findByDefendantId(defendantId)).isNull()
  }

  @Test
  fun `should not store defendant with missing firstName, middleName and Date of birth`() {
    val defendantId = randomDefendantId()
    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = defendantId, firstName = "", middleName = "", lastName = randomName(), dateOfBirth = "", isPerson = true),
        ),
      ),
    )

    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute("commonplatform.case.received"))
    assertThat(sqsMessage.message.contains("cprUUID")).isFalse()
    assertThat(sqsMessage.message.contains(defendantId)).isTrue()

    assertThat(personRepository.findByDefendantId(defendantId)).isNull()
  }

  @Test
  fun `should republish large message and not save when organisation`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    val organizationDefendantId = randomDefendantId()
    val defendantId = randomDefendantId()
    val organisations = (1..250).map { CommonPlatformHearingSetup(isPerson = false) }
    val person = CommonPlatformHearingSetup(defendantId = defendantId)
    val organization = CommonPlatformHearingSetup(defendantId = organizationDefendantId, isPerson = false)
    val largeMessage = commonPlatformHearing(organisations + person + organization)

    val (messageStoredInS3) = publishAndReceiveLargeMessage(largeMessage)

    val occurrenceOfCprUUId = messageStoredInS3.split("cprUUID").size - 1

    val defendant = awaitNotNullPerson {
      personRepository.findByDefendantId(defendantId)
    }

    assertThat(personRepository.findByDefendantId(organizationDefendantId)).isNull()
    assertThat(occurrenceOfCprUUId).isEqualTo(1)
    assertThat(messageStoredInS3.contains(defendant.personKey?.personUUID.toString())).isEqualTo(true)
  }

  @Test
  fun `should republish message to court topic including the cpr uuid`() {
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

    val person = awaitNotNullPerson {
      personRepository.findByDefendantId(defendantId)
    }
    assertThat(commonPlatformHearing.contains("cprUUID")).isEqualTo(true)
    assertThat(commonPlatformHearing.contains(person.personKey?.personUUID.toString())).isEqualTo(true)
    assertThat(sqsMessage.message.contains("pncId")).isTrue()
    assertThat(sqsMessage.message.contains("croNumber")).isTrue()
    assertThat(commonPlatformHearingAttributes?.messageType?.value).isEqualTo(COMMON_PLATFORM_HEARING.name)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId),
    )
  }

  @Test
  fun `should publish large message to CPR court topic including the cpr uuid`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    val defendantId = randomDefendantId()

    val (messageBody, sqsMessage) = publishAndReceiveLargeMessage(largeCommonPlatformHearing(defendantId))
    val person = awaitNotNullPerson {
      personRepository.findByDefendantId(defendantId)
    }

    assertThat(sqsMessage?.messageAttributes?.eventType).isEqualTo(MessageAttribute(LARGE_CASE_EVENT_TYPE))
    assertThat(sqsMessage?.messageAttributes?.hearingEventType).isEqualTo(MessageAttribute("ConfirmedOrUpdated"))
    assertThat(messageBody.contains(defendantId)).isEqualTo(true)
    assertThat(messageBody.contains("cprUUID")).isEqualTo(true)
    assertThat(messageBody.contains(person.personKey?.personUUID.toString())).isEqualTo(true)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId),
    )
  }

  @Test
  fun `should preserve pnc and cro if originally set then missing from an update`() {
    val defendantId = randomDefendantId()
    val cro = randomCro()
    val pnc = randomPnc()

    stubPersonMatchUpsert()
    stubPersonMatchScores()

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, cro = cro, pnc = pnc))),
    )

    awaitNotNullPerson {
      personRepository.findByDefendantId(defendantId)
    }

    purgeQueueAndDlq(testOnlyCourtEventsQueue)

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, croMissing = true, pncMissing = true))),
    )

    val updatedPerson = awaitNotNullPerson {
      personRepository.findByDefendantId(defendantId)
    }

    assertThat(updatedPerson.getPnc()).isEqualTo(pnc)
    assertThat(updatedPerson.getCro()).isEqualTo(cro)
    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute("commonplatform.case.received"))
    assertThat(sqsMessage.message.contains("pncId")).isFalse()
    assertThat(sqsMessage.message.contains("croNumber")).isFalse()
  }

  @Test
  fun `should return empty strings for pnc and cro if missing`() {
    val defendantId = randomDefendantId()

    stubPersonMatchUpsert()
    stubPersonMatchScores()

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, cro = "", pnc = ""))),
    )

    awaitNotNullPerson {
      personRepository.findByDefendantId(defendantId)
    }

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, croMissing = true, pncMissing = true))),
    )

    val updatedPerson = awaitNotNullPerson {
      personRepository.findByDefendantId(defendantId)
    }

    assertThat(updatedPerson.getPnc()).isNull()
    assertThat(updatedPerson.getCro()).isNull()
  }

  private fun putLargeMessageBodyIntoS3(message: String) {
    val s3Key = UUID.randomUUID().toString()
    val incomingMessageFromS3 = message.toByteArray(Charset.forName("UTF8"))
    val putObjectRequest = PutObjectRequest.builder().bucket(s3Bucket).key(s3Key).build()
    s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromBytes(incomingMessageFromS3)).get()

    assertThat(incomingMessageFromS3.size).isGreaterThan(256 * 1024)

    publishLargeCommonPlatformMessage(
      largeCommonPlatformMessage(s3Key, s3Bucket),
    )
  }

  private fun publishAndReceiveLargeMessage(message: String): Pair<String, SQSMessage?> {
    putLargeMessageBodyIntoS3(message)

    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())
    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }
    val messageBody = objectMapper.readValue(sqsMessage?.message, ArrayList::class.java)
    val (s3Key, s3BucketName) = objectMapper.readValue(objectMapper.writeValueAsString(messageBody[1]), LargeMessageBody::class.java)
    val body = s3AsyncClient.getObject(
      GetObjectRequest.builder().key(s3Key).bucket(s3BucketName).build(),
      AsyncResponseTransformer.toBytes(),
    ).join().asUtf8String()

    return Pair(body, sqsMessage)
  }

  @Nested
  inner class EventLog {

    @Test
    fun `should save details to event log on defendant create`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val pnc = randomPnc()
      val cro = randomCro()
      val defendantId = randomDefendantId()
      val firstName = randomName()
      val lastName = randomName()
      val aliasFirstName = randomName()
      val aliasLastName = randomName()

      publishCommonPlatformMessage(
        commonPlatformHearing(
          listOf(
            CommonPlatformHearingSetup(
              pnc = pnc,
              firstName = firstName,
              lastName = lastName,
              cro = cro,
              defendantId = defendantId,
              aliases = listOf(
                CommonPlatformHearingSetupAlias(firstName = aliasFirstName, lastName = aliasLastName),
              ),
            ),
          ),
        ),
      )

      checkEventLog(defendantId, CPRLogEvents.CPR_RECORD_CREATED) { eventLogs ->
        assertThat(eventLogs.size).isEqualTo(1)
        val createdLog = eventLogs.first()
        assertThat(createdLog.pncs).isEqualTo(arrayOf(pnc))
        assertThat(createdLog.cros).isEqualTo(arrayOf(cro))
        assertThat(createdLog.firstName).isEqualTo(firstName)
        assertThat(createdLog.lastName).isEqualTo(lastName)
        assertThat(createdLog.sourceSystemId).isEqualTo(defendantId)
        assertThat(createdLog.sourceSystem).isEqualTo(COMMON_PLATFORM)
        assertThat(createdLog.firstNameAliases).isEqualTo(arrayOf(aliasFirstName))
        assertThat(createdLog.lastNameAliases).isEqualTo(arrayOf(aliasLastName))
        assertThat(createdLog.personUUID).isNotNull()
        assertThat(createdLog.uuidStatusType).isEqualTo(ACTIVE)
      }
      checkEventLogExist(defendantId, CPRLogEvents.CPR_UUID_CREATED)
    }
  }
}
