package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.commonplatform

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.personrecord.extensions.getHome
import uk.gov.justice.digital.hmpps.personrecord.extensions.getMobile
import uk.gov.justice.digital.hmpps.personrecord.extensions.getPNCs
import uk.gov.justice.digital.hmpps.personrecord.extensions.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.queue.LARGE_CASE_EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupContact
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.largeCommonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.largeCommonPlatformMessage
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitle
import java.nio.charset.Charset
import java.time.LocalDateTime.now
import java.util.UUID

class CommonPlatformCourtEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  lateinit var s3AsyncClient: S3AsyncClient

  @Value($$"${aws.court-message-bucket-name}")
  lateinit var s3Bucket: String

  @Test
  fun `should update an existing person record from common platform message`() {
    stubPersonMatchUpsert()
    val defendantId = randomDefendantId()
    val pnc = randomLongPnc()
    val cro = randomCro()
    val lastName = randomName()
    val nationality = randomCommonPlatformNationalityCode()
    val additionalNationality = randomCommonPlatformNationalityCode()
    val masterDefendantId = randomDefendantId()

    stubNoMatchesPersonMatch()

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(gender = randomCommonPlatformSexCode().key, pnc = pnc, lastName = lastName, cro = cro, defendantId = defendantId, nationalityCode = nationality, additionalNationalityCode = additionalNationality, masterDefendantId = randomDefendantId()))),
    )
    val newPerson = awaitNotNull { personRepository.findByDefendantId(defendantId) }
    checkNationalities(newPerson, nationality, additionalNationality)
    val changedLastName = randomName()
    val changedSexCode = randomCommonPlatformSexCode()
    val changedNationality = randomCommonPlatformNationalityCode()
    val changedAdditionalNationality = randomCommonPlatformNationalityCode()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(gender = changedSexCode.key, pnc = pnc, lastName = changedLastName, cro = cro, defendantId = defendantId, nationalityCode = changedNationality, additionalNationalityCode = changedAdditionalNationality, masterDefendantId = masterDefendantId))),
    )

    awaitAssert {
      val updatedPersonEntity = personRepository.findByDefendantId(defendantId)!!
      assertThat(updatedPersonEntity.getPrimaryName().lastName).isEqualTo(changedLastName)
      assertThat(updatedPersonEntity.getPrimaryName().sexCode).isEqualTo(changedSexCode.value)
      assertThat(updatedPersonEntity.getPnc()).isEqualTo(pnc)
      assertThat(updatedPersonEntity.getCro()).isEqualTo(cro)
      assertThat(updatedPersonEntity.masterDefendantId).isEqualTo(masterDefendantId)
      checkNationalities(updatedPersonEntity, changedNationality, changedAdditionalNationality)
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
    val pnc = randomLongPnc()
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
      religion = person.religion,
      matchId = UUID.randomUUID(),
      cId = person.cId,
      lastModified = now(),
    )
    personEntity.personKey = personKey
    personKeyRepository.saveAndFlush(personKey)
    personRepository.saveAndFlush(personEntity)
    stubNoMatchesPersonMatch(matchId = personEntity.matchId)

    val changedLastName = randomName()
    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            pnc = pnc,
            lastName = changedLastName,
            cro = cro,
            defendantId = defendantId,
            ethnicity = ethnicity,
          ),
        ),
      ),
    )

    awaitAssert {
      val updatedPersonEntity = personRepository.findByDefendantId(defendantId)!!
      val storedEthnicity = ethnicity.getCommonPlatformEthnicity()
      assertThat(updatedPersonEntity.getPrimaryName().lastName).isEqualTo(changedLastName)
      assertThat(updatedPersonEntity.getPnc()).isEqualTo(pnc)
      assertThat(updatedPersonEntity.getCro()).isEqualTo(cro)
      assertThat(updatedPersonEntity.ethnicityCodeLegacy?.code).isEqualTo(storedEthnicity.code)
      assertThat(updatedPersonEntity.ethnicityCodeLegacy?.description).isEqualTo(storedEthnicity.description)

//      assertThat(updatedPersonEntity.ethnicityCode).isEqualTo(null) -- TODO -reintroduce this after we fix the titleCode mapping
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
    val firstPnc = randomLongPnc()
    val firstName = randomName()
    val lastName = randomName()
    val secondPnc = randomLongPnc()

    val firstDefendantId = randomDefendantId()
    val firstDefendantNINumber = randomNationalInsuranceNumber()
    val secondDefendantId = randomDefendantId()

    val buildingName = randomName()
    val buildingNumber = randomBuildingNumber()
    val thoroughfareName = randomName()
    val dependentLocality = randomName()
    val postTown = randomName()
    val postcode = randomPostcode()
    val title = randomTitle()
    val ethnicity = randomCommonPlatformEthnicity()

    val firstNationality = randomCommonPlatformNationalityCode()
    val firstAdditionalNationality = randomCommonPlatformNationalityCode()
    val secondNationality = randomCommonPlatformNationalityCode()

    val firstSexCode = randomCommonPlatformSexCode()
    val secondSexCode = randomCommonPlatformSexCode()

    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            gender = firstSexCode.key,
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
            additionalNationalityCode = firstAdditionalNationality,
            ethnicity = ethnicity,
            nationalInsuranceNumber = firstDefendantNINumber,
          ),
          CommonPlatformHearingSetup(
            gender = secondSexCode.key,
            pnc = secondPnc,
            defendantId = secondDefendantId,
            contact = CommonPlatformHearingSetupContact(),
            nationalityCode = secondNationality,
            address =
            CommonPlatformHearingSetupAddress(buildingName = buildingName, buildingNumber = buildingNumber, thoroughfareName = thoroughfareName, dependentLocality = dependentLocality, postTown = postTown, postcode = postcode),
          ),
        ),
      ),

    )

    val firstPerson = awaitNotNull {
      personRepository.findByDefendantId(firstDefendantId)
    }

    val secondPerson = awaitNotNull {
      personRepository.findByDefendantId(secondDefendantId)
    }

    assertThat(firstPerson.getPnc()).isEqualTo(firstPnc)
    assertThat(firstPerson.personKey).isNotNull()
    assertThat(firstPerson.masterDefendantId).isEqualTo(firstDefendantId)
    val storedTitle = title.getTitle()
    assertThat(firstPerson.getPrimaryName().titleCodeLegacy?.code).isEqualTo(storedTitle.code)
    assertThat(firstPerson.getPrimaryName().titleCodeLegacy?.description).isEqualTo(storedTitle.description)
//    assertThat(firstPerson.getPrimaryName().titleCode).isEqualTo(null) -- TODO -reintroduce this after we fix the titleCode mapping
    assertThat(firstPerson.getPrimaryName().firstName).isEqualTo(firstName)
    assertThat(firstPerson.getPrimaryName().middleNames).isEqualTo("mName1 mName2")
    assertThat(firstPerson.getPrimaryName().lastName).isEqualTo(lastName)
    assertThat(firstPerson.getPrimaryName().sexCode).isEqualTo(firstSexCode.value)
    assertThat(firstPerson.contacts).isEmpty()
    checkNationalities(firstPerson, firstAdditionalNationality, firstNationality)

    assertThat(firstPerson.getAliases().size).isEqualTo(2)
    assertThat(firstPerson.getAliases()[0].titleCodeLegacy).isNull()
//    assertThat(firstPerson.getAliases()[0].titleCode).isNull() -- TODO -reintroduce this after we fix the titleCode mapping
    assertThat(firstPerson.getAliases()[0].firstName).isEqualTo("aliasFirstName1")
    assertThat(firstPerson.getAliases()[0].lastName).isEqualTo("aliasLastName1")
    assertThat(firstPerson.getAliases()[1].titleCodeLegacy).isNull()
//    assertThat(firstPerson.getAliases()[1].titleCode).isNull() -- TODO -reintroduce this after we fix the titleCode mapping
    assertThat(firstPerson.getAliases()[1].firstName).isEqualTo("aliasFirstName2")
    assertThat(firstPerson.getAliases()[1].lastName).isEqualTo("aliasLastName2")

    val ethnicityCode = ethnicity.getCommonPlatformEthnicity()
    assertThat(firstPerson.ethnicityCodeLegacy?.code).isEqualTo(ethnicityCode.code)
    assertThat(firstPerson.ethnicityCodeLegacy?.description).isEqualTo(ethnicityCode.description)
    assertThat(firstPerson.references.getType(NATIONAL_INSURANCE_NUMBER).first()).isEqualTo(firstDefendantNINumber)

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
    assertThat(secondPerson.addresses[0].countryCode).isNull()
    assertThat(secondPerson.addresses[0].uprn).isNull()
    assertThat(secondPerson.getPnc()).isEqualTo(secondPnc)
    assertThat(secondPerson.contacts.size).isEqualTo(3)
    assertThat(secondPerson.contacts.getHome()?.contactValue).isEqualTo("0207345678")
    assertThat(secondPerson.contacts.getMobile()?.contactValue).isEqualTo("078590345677")
    assertThat(secondPerson.masterDefendantId).isEqualTo(secondDefendantId)
    assertThat(secondPerson.getPrimaryName().sexCode).isEqualTo(secondSexCode.value)
    checkNationalities(secondPerson, secondNationality)
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

    val personWithEmptyPnc = awaitNotNull {
      personRepository.findByDefendantId(firstDefendantId)
    }
    assertThat(personWithEmptyPnc.references.getPNCs()).isEmpty()

    val personWithNullPnc = personRepository.findByDefendantId(secondDefendantId)
    assertThat(personWithNullPnc?.references?.getPNCs()).isEmpty()
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

    awaitNotNull { personRepository.findByDefendantId(defendantId) }
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

    val defendant = awaitNotNull {
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

    val person = awaitNotNull {
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
    val person = awaitNotNull {
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
  fun `should preserve pnc if originally set then missing from an update`() {
    val defendantId = randomDefendantId()
    val pnc = randomLongPnc()

    stubPersonMatchUpsert()
    stubPersonMatchScores()

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, pnc = pnc))),
    )

    awaitNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    purgeQueueAndDlq(testOnlyCourtEventsQueue)

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, pncMissing = true))),
    )

    val updatedPerson = awaitNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    assertThat(updatedPerson.getPnc()).isEqualTo(pnc)
    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute("commonplatform.case.received"))
    assertThat(sqsMessage.message.contains("pncId")).isFalse()
  }

  @Test
  fun `should preserve cro if originally set then missing from an update`() {
    val defendantId = randomDefendantId()
    val cro = randomCro()

    stubPersonMatchUpsert()
    stubPersonMatchScores()

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, cro = cro))),
    )

    awaitNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    purgeQueueAndDlq(testOnlyCourtEventsQueue)

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, croMissing = true))),
    )

    val updatedPerson = awaitNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    assertThat(updatedPerson.getCro()).isEqualTo(cro)
    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute("commonplatform.case.received"))
    assertThat(sqsMessage.message.contains("croId")).isFalse()
  }

  @Test
  fun `should return empty strings for pnc and cro if missing`() {
    val defendantId = randomDefendantId()

    stubPersonMatchUpsert()
    stubPersonMatchScores()

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, cro = "", pnc = ""))),
    )

    awaitNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, croMissing = true, pncMissing = true))),
    )

    val updatedPerson = awaitNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    assertThat(updatedPerson.getPnc()).isNull()
    assertThat(updatedPerson.getCro()).isNull()
  }

  @Test
  fun `should return null for pnc and cro if provided but are set to empty strings`() {
    val defendantId = randomDefendantId()

    stubPersonMatchUpsert()
    stubPersonMatchScores()

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, cro = randomCro(), pnc = randomLongPnc()))),
    )

    awaitNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, cro = "", pnc = ""))),
    )

    val updatedPerson = awaitNotNull {
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

  private fun checkNationalities(
    person: PersonEntity,
    vararg nationalities: String,
  ) {
    assertThat(person.nationalities.size).isEqualTo(nationalities.size)
    val expected = nationalities.map { NationalityCode.fromCommonPlatformMapping(it) }
    val actualNationalitiesCodes = person.nationalities.map { it.nationalityCode }
    assertThat(actualNationalitiesCodes).containsAll(expected)
  }

  @Nested
  inner class Address {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
    }

    @Test
    fun `should add new address to current and move old to previous`() {
      val defendantId = randomDefendantId()

      val postcode1 = randomPostcode()

      publishCommonPlatformMessage(
        commonPlatformHearing(
          listOf(
            CommonPlatformHearingSetup(
              defendantId = defendantId,
              address = CommonPlatformHearingSetupAddress(
                buildingName = randomName(),
                buildingNumber = randomBuildingNumber(),
                thoroughfareName = randomName(),
                dependentLocality = randomName(),
                postTown = "",
                postcode = postcode1,
              ),
            ),
          ),
        ),
      )

      val person = awaitNotNull { personRepository.findByDefendantId(defendantId) }
      assertThat(person.addresses).hasSize(1)
      assertThat(person.addresses.getPrimary().first().postcode).isEqualTo(postcode1)

      val postcode2 = randomPostcode()

      publishCommonPlatformMessage(
        commonPlatformHearing(
          listOf(
            CommonPlatformHearingSetup(
              defendantId = defendantId,
              address = CommonPlatformHearingSetupAddress(
                buildingName = randomName(),
                buildingNumber = randomBuildingNumber(),
                thoroughfareName = randomName(),
                dependentLocality = randomName(),
                postTown = "",
                postcode = postcode2,
              ),
            ),
          ),
        ),
      )

      val updatedPerson = awaitNotNull {
        personRepository.findByDefendantId(defendantId)
      }
      assertThat(updatedPerson.addresses).hasSize(2)
      assertThat(updatedPerson.addresses.getPrimary().first().postcode).isEqualTo(postcode2)
      assertThat(updatedPerson.addresses.getPrevious().first().postcode).isEqualTo(postcode1)
    }

    @Test
    fun `should not move primary address to previous if duplicate address on update`() {
      val defendantId = randomDefendantId()

      val address = CommonPlatformHearingSetupAddress(
        buildingName = randomName(),
        buildingNumber = randomBuildingNumber(),
        thoroughfareName = randomName(),
        dependentLocality = randomName(),
        postTown = randomName(),
        postcode = randomPostcode(),
      )

      publishCommonPlatformMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, address = address))))

      val person = awaitNotNull { personRepository.findByDefendantId(defendantId) }
      assertThat(person.addresses).hasSize(1)
      assertThat(person.addresses.getPrimary().first().postcode).isEqualTo(address.postcode)

      publishCommonPlatformMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, address = address))))

      val updatedPerson = awaitNotNull { personRepository.findByDefendantId(defendantId) }
      assertThat(updatedPerson.addresses).hasSize(1)
      assertThat(updatedPerson.addresses.getPrimary().first().postcode).isEqualTo(address.postcode)
    }

    @Test
    fun `should move previous address to primary if update contains a previous address`() {
      val defendantId = randomDefendantId()

      val address = CommonPlatformHearingSetupAddress(
        buildingName = randomName(),
        buildingNumber = randomBuildingNumber(),
        thoroughfareName = randomName(),
        dependentLocality = randomName(),
        postTown = randomName(),
        postcode = randomPostcode(),
      )

      publishCommonPlatformMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, address = address))))

      val person = awaitNotNull { personRepository.findByDefendantId(defendantId) }
      assertThat(person.addresses).hasSize(1)
      assertThat(person.addresses.getPrimary().first().postcode).isEqualTo(address.postcode)

      val secondAddress = CommonPlatformHearingSetupAddress(
        buildingName = randomName(),
        buildingNumber = randomBuildingNumber(),
        thoroughfareName = randomName(),
        dependentLocality = randomName(),
        postTown = randomName(),
        postcode = randomPostcode(),
      )

      publishCommonPlatformMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, address = secondAddress))))

      val updatedPerson = awaitNotNull { personRepository.findByDefendantId(defendantId) }
      assertThat(updatedPerson.addresses).hasSize(2)
      assertThat(updatedPerson.addresses.getPrimary().first().postcode).isEqualTo(secondAddress.postcode)
      assertThat(updatedPerson.addresses.getPrevious().first().postcode).isEqualTo(address.postcode)

      publishCommonPlatformMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, address = address))))

      val reUpdatedPerson = awaitNotNull { personRepository.findByDefendantId(defendantId) }
      assertThat(reUpdatedPerson.addresses).hasSize(2)
      assertThat(reUpdatedPerson.addresses.getPrimary().first().postcode).isEqualTo(address.postcode)
      assertThat(reUpdatedPerson.addresses.getPrevious().first().postcode).isEqualTo(secondAddress.postcode)
    }

    @Test
    fun `should move old address with record type of null to previous`() {
      val person = createPersonWithNewKey(createRandomCommonPlatformPersonDetails())
      val postcode = randomPostcode()
      person.addresses.add(
        AddressEntity(
          buildingNumber = randomBuildingNumber(),
          postcode = postcode,
          recordType = null,
          person = person,
        ),
      )

      val existingPerson = personRepository.save(person)
      assertThat(existingPerson.addresses).hasSize(1)
      assertThat(existingPerson.addresses.first().recordType).isEqualTo(null)
      assertThat(existingPerson.addresses.first().postcode).isEqualTo(postcode)

      val updatedAddress = CommonPlatformHearingSetupAddress(
        buildingName = randomName(),
        buildingNumber = randomBuildingNumber(),
        thoroughfareName = randomName(),
        dependentLocality = randomName(),
        postTown = randomName(),
        postcode = randomPostcode(),
      )

      publishCommonPlatformMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = existingPerson.defendantId!!, address = updatedAddress))))

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to existingPerson.defendantId!!),
      )

      val updatedPerson = awaitNotNull { personRepository.findByDefendantId(existingPerson.defendantId!!) }
      assertThat(updatedPerson.addresses).hasSize(2)
      assertThat(updatedPerson.addresses.getPrimary().first().postcode).isEqualTo(updatedAddress.postcode)
      assertThat(updatedPerson.addresses.getPrevious().first().postcode).isEqualTo(postcode)
    }

    @Test
    fun `should move primary to previous and not store blank address`() {
      val defendantId = randomDefendantId()

      val address = CommonPlatformHearingSetupAddress(
        buildingName = randomName(),
        buildingNumber = randomBuildingNumber(),
        thoroughfareName = randomName(),
        dependentLocality = randomName(),
        postTown = randomName(),
        postcode = randomPostcode(),
      )

      publishCommonPlatformMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, address = address))))
      awaitNotNull { personRepository.findByDefendantId(defendantId) }

      val blankAddress = CommonPlatformHearingSetupAddress(
        buildingName = "",
        buildingNumber = "",
        thoroughfareName = "",
        dependentLocality = "",
        postTown = "",
        postcode = "",
      )

      publishCommonPlatformMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, address = blankAddress))))

      val updatedPerson = awaitNotNull { personRepository.findByDefendantId(defendantId) }
      assertThat(updatedPerson.addresses).hasSize(1)
      assertThat(updatedPerson.addresses.getPrevious().first().postcode).isEqualTo(address.postcode)
    }
  }

  @Nested
  inner class EventLog {

    @Test
    fun `should save details to event log on defendant create`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val pnc = randomLongPnc()
      val cro = randomCro()
      val defendantId = randomDefendantId()
      val firstName = randomName()
      val lastName = randomName()
      val aliasFirstName = randomName()
      val aliasLastName = randomName()
      val masterDefendantId = randomDefendantId()

      publishCommonPlatformMessage(
        commonPlatformHearing(
          listOf(
            CommonPlatformHearingSetup(
              pnc = pnc,
              firstName = firstName,
              lastName = lastName,
              masterDefendantId = masterDefendantId,
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
        assertThat(createdLog.masterDefendantId).isEqualTo(masterDefendantId)
      }
      checkEventLogExist(defendantId, CPRLogEvents.CPR_UUID_CREATED)
    }
  }
  private fun List<AddressEntity>.getPrimary(): List<AddressEntity> = this.getByType(AddressRecordType.PRIMARY)
  private fun List<AddressEntity>.getPrevious(): List<AddressEntity> = this.getByType(AddressRecordType.PREVIOUS)
  private fun List<AddressEntity>.getByType(type: AddressRecordType): List<AddressEntity> = this.filter { it.recordType == type }
}
