package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DETAILS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationality
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupSentences
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit.SECONDS

class ProbationEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `creates person when when new offender created event is published`() {
    val crn = randomCRN()
    val title = randomName()
    val prisonNumber = randomPrisonNumber()
    val firstName = randomName()
    val lastName = randomName()
    val pnc = randomPnc()
    val cro = randomCro()
    val addressStartDate = randomDate()
    val addressEndDate = randomDate()
    val ethnicity = randomEthnicity()
    val nationality = randomNationality()
    val sentenceDate = randomDate()

    val apiResponse = ApiResponseSetup(
      crn = crn,
      pnc = pnc,
      title = title,
      firstName = firstName,
      lastName = lastName,
      prisonNumber = prisonNumber,
      cro = cro,
      addresses = listOf(
        ApiResponseSetupAddress(noFixedAbode = true, addressStartDate, addressEndDate, postcode = "LS1 1AB", fullAddress = "abc street"),
        ApiResponseSetupAddress(postcode = "M21 9LX", fullAddress = "abc street"),
      ),
      ethnicity = ethnicity,
      nationality = nationality,
      sentences = listOf(ApiResponseSetupSentences(sentenceDate)),
    )
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, apiResponse)

    val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }

    assertThat(personEntity.personKey).isNotNull()
    assertThat(personEntity.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
    assertThat(personEntity.firstName).isEqualTo(firstName)
    assertThat(personEntity.middleNames).isEqualTo("PreferredMiddleName")
    assertThat(personEntity.lastName).isEqualTo(lastName)
    assertThat(personEntity.title).isEqualTo(title)
    assertThat(personEntity.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
    assertThat(personEntity.crn).isEqualTo(crn)
    assertThat(personEntity.ethnicity).isEqualTo(ethnicity)
    assertThat(personEntity.nationality).isEqualTo(nationality)
    assertThat(personEntity.sentenceInfo[0].sentenceDate).isEqualTo(sentenceDate)
    assertThat(personEntity.references.getType(IdentifierType.CRO).first().identifierValue).isEqualTo(cro)
    assertThat(personEntity.pseudonyms.size).isEqualTo(1)
    assertThat(personEntity.pseudonyms[0].firstName).isEqualTo("FirstName")
    assertThat(personEntity.pseudonyms[0].middleNames).isEqualTo("MiddleName")
    assertThat(personEntity.pseudonyms[0].lastName).isEqualTo("LastName")
    assertThat(personEntity.pseudonyms[0].dateOfBirth).isEqualTo(LocalDate.of(2024, 5, 30))
    assertThat(personEntity.addresses.size).isEqualTo(2)
    assertThat(personEntity.addresses[0].noFixedAbode).isEqualTo(true)
    assertThat(personEntity.addresses[0].startDate).isEqualTo(addressStartDate)
    assertThat(personEntity.addresses[0].endDate).isEqualTo(addressEndDate)
    assertThat(personEntity.addresses[0].postcode).isEqualTo("LS1 1AB")
    assertThat(personEntity.addresses[0].fullAddress).isEqualTo("abc street")
    assertThat(personEntity.addresses[0].type).isEqualTo(null)
    assertThat(personEntity.addresses[1].noFixedAbode).isEqualTo(null)
    assertThat(personEntity.addresses[1].postcode).isEqualTo("M21 9LX")
    assertThat(personEntity.addresses[1].fullAddress).isEqualTo("abc street")
    assertThat(personEntity.addresses[1].type).isEqualTo(null)
    assertThat(personEntity.currentlyManaged).isEqualTo(null)
    assertThat(personEntity.contacts.size).isEqualTo(3)
    assertThat(personEntity.contacts[0].contactType).isEqualTo(ContactType.HOME)
    assertThat(personEntity.contacts[0].contactValue).isEqualTo("01234567890")
    assertThat(personEntity.contacts[1].contactType).isEqualTo(ContactType.MOBILE)
    assertThat(personEntity.contacts[1].contactValue).isEqualTo("01234567890")
    assertThat(personEntity.contacts[2].contactType).isEqualTo(ContactType.EMAIL)
    assertThat(personEntity.contacts[2].contactValue).isEqualTo("test@gmail.com")

    checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should link person to an existing NOMIS record`() {
    val crn = randomCRN()
    val prisonNumber = randomPrisonNumber()
    val firstName = randomName()
    val pnc = randomPnc()
    val cro = randomCro()
    val person = Person(
      prisonNumber = prisonNumber,
      references = listOf(Reference.from(IdentifierType.PNC, pnc), Reference.from(IdentifierType.CRO, cro)),
      addresses = listOf(Address(postcode = "LS1 1AB")),
      sourceSystem = NOMIS,
    )
    val personKeyEntity = createPersonKey()
    createPerson(person, personKeyEntity = personKeyEntity)

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)

    val apiResponse = ApiResponseSetup(
      crn = crn,
      pnc = pnc,
      firstName = firstName,
      prisonNumber = prisonNumber,
      cro = cro,
      addresses = listOf(ApiResponseSetupAddress(postcode = "LS1 1AB", fullAddress = "abc street"), ApiResponseSetupAddress(postcode = "M21 9LX", fullAddress = "abc street")),
    )
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, apiResponse)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to DELIUS.name,
        "RECORD_COUNT" to "1",
        "UUID_COUNT" to "1",
        "HIGH_CONFIDENCE_COUNT" to "1",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )
    checkTelemetry(
      CPR_CANDIDATE_RECORD_FOUND_UUID,
      mapOf(
        "SOURCE_SYSTEM" to DELIUS.name,
        "CLUSTER_SIZE" to "1",
        "UUID" to personKeyEntity.personId.toString(),
      ),
    )
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

    val personKey = personKeyRepository.findByPersonId(personKeyEntity.personId)
    assertThat(personKey?.personEntities?.size).isEqualTo(2)
  }

  @Test
  fun `should write offender without PNC if PNC is missing`() {
    val crn = randomCRN()
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = null))
    val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }

    assertThat(personEntity.references.getType(IdentifierType.PNC)).isEqualTo(emptyList<ReferenceEntity>())

    checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should create two offenders with same prisonNumber but different CRNs`() {
    val prisonNumber: String = randomPrisonNumber()
    val crn = randomCRN()
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, prisonNumber = prisonNumber))
    awaitNotNullPerson { personRepository.findByCrn(crn) }
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

    val nextCrn = randomCRN()
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = nextCrn, prisonNumber = prisonNumber))
    awaitNotNullPerson { personRepository.findByCrn(nextCrn) }

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to nextCrn))
    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should handle new offender details with an empty pnc`() {
    val crn = randomCRN()
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = ""))

    val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }

    assertThat(personEntity.references.getType(IdentifierType.PNC)).isEqualTo(emptyList<ReferenceEntity>())

    checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should not push 404 to dead letter queue but discard message instead`() {
    val crn = randomCRN()
    stub404Response(probationUrl(crn))

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, personReference = personReference, additionalInformation = null)
    publishDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    expectNoMessagesOn(probationEventsQueue)
    expectNoMessagesOnDlq(probationEventsQueue)
    checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"), 1)
  }

  @Test
  fun `should retry on 500 error`() {
    val crn = randomCRN()
    stub500Response(probationUrl(crn), "next request will succeed", "retry")
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = randomPnc()), scenario = "retry", currentScenarioState = "next request will succeed")

    expectNoMessagesOn(probationEventsQueue)
    expectNoMessagesOnDlq(probationEventsQueue)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"), 1)
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should log when message processing fails`() {
    val crn = randomCRN()
    stub500Response(probationUrl(crn), STARTED, "failure")
    stub500Response(probationUrl(crn), STARTED, "failure")
    stub500Response(probationUrl(crn), STARTED, "failure")
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))

    val domainEvent = DomainEvent(
      eventType = NEW_OFFENDER_CREATED,
      personReference = personReference,
      additionalInformation = null,
    )
    val messageId = publishDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    probationEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationEventsQueue!!.queueUrl).build(),
    ).get()
    probationEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationEventsQueue!!.dlqUrl).build(),
    ).get()

    checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"), 1)
    checkTelemetry(
      MESSAGE_PROCESSING_FAILED,
      mapOf(
        "SOURCE_SYSTEM" to "DELIUS",
        EventKeys.EVENT_TYPE.toString() to NEW_OFFENDER_CREATED,
        EventKeys.MESSAGE_ID.toString() to messageId,
      ),
    )
  }

  @ParameterizedTest
  @ValueSource(strings = [OFFENDER_DETAILS_CHANGED, OFFENDER_ALIAS_CHANGED, OFFENDER_ADDRESS_CHANGED])
  fun `should process probation events successfully`(event: String) {
    val pnc = randomPnc()
    val crn = randomCRN()
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = pnc))
    val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }
    assertThat(personEntity.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
    checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

    val changedPnc = randomPnc()
    probationEventAndResponseSetup(event, ApiResponseSetup(crn = crn, pnc = changedPnc))
    checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to event, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

    val updatedPersonEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }
    assertThat(updatedPersonEntity.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(changedPnc)

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to personEntity.personKey?.personId.toString()),
    )
  }

  @Test
  fun `should log event to EventLogging table when new offender created`() {
    val crn = randomCRN()
    val title = randomName()
    val prisonNumber = randomPrisonNumber()
    val firstName = randomName()
    val lastName = randomName()
    val pnc = randomPnc()
    val cro = randomCro()
    val addressStartDate = randomDate()
    val addressEndDate = randomDate()
    val ethnicity = randomEthnicity()
    val nationality = randomNationality()
    val sentenceDate = randomDate()

    val apiResponse = ApiResponseSetup(
      crn = crn,
      pnc = pnc,
      title = title,
      firstName = firstName,
      lastName = lastName,
      prisonNumber = prisonNumber,
      cro = cro,
      addresses = listOf(
        ApiResponseSetupAddress(noFixedAbode = true, addressStartDate, addressEndDate, postcode = "LS1 1AB", fullAddress = "abc street"),
        ApiResponseSetupAddress(postcode = "M21 9LX", fullAddress = "abc street"),
      ),
      ethnicity = ethnicity,
      nationality = nationality,
      sentences = listOf(ApiResponseSetupSentences(sentenceDate)),
    )

    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, apiResponse)

    await.atMost(4, SECONDS) untilNotNull { personRepository.findByCrn(crn)?.personKey }
    val personEntity = personRepository.findByCrn(crn)!!
    val processedDataDTO = Person.from(personEntity)
    val processedData = objectMapper.writeValueAsString(processedDataDTO)

    val loggedEvent = await.atMost(4, SECONDS) untilNotNull {
      eventLoggingRepository.findFirstBySourceSystemIdOrderByEventTimestampDesc(crn)
    }
    assertThat(loggedEvent.eventType).isEqualTo(NEW_OFFENDER_CREATED)
    assertThat(loggedEvent.sourceSystemId).isEqualTo(crn)
    assertThat(loggedEvent.sourceSystem).isEqualTo(DELIUS.name)
    assertThat(loggedEvent.eventTimestamp).isBefore(LocalDateTime.now())
    assertThat(loggedEvent.beforeData).isNull()
    assertThat(loggedEvent.processedData).isEqualTo(processedData)

    assertThat(loggedEvent.uuid).isEqualTo(personEntity.personKey?.personId.toString())
  }

  @Test
  fun `should log event to EventLogging table when offender updated`() {
    val firstName = randomName()
    val crn = randomCRN()

    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, firstName = firstName))

    val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }
    val beforeDataDTO = Person.from(personEntity)
    val beforeData = objectMapper.writeValueAsString(beforeDataDTO)

    val changedFirstName = randomName()
    probationEventAndResponseSetup(OFFENDER_DETAILS_CHANGED, ApiResponseSetup(crn = crn, firstName = changedFirstName))

    awaitAssert { assertThat(personRepository.findByCrn(crn)?.firstName).isEqualTo(changedFirstName) }
    val updatedPersonEntity = personRepository.findByCrn(crn)!!
    val processedDataDTO = Person.from(updatedPersonEntity)
    val processedData = objectMapper.writeValueAsString(processedDataDTO)

    val loggedEvent = awaitNotNullEventLog(crn, OFFENDER_DETAILS_CHANGED)

    assertThat(loggedEvent.sourceSystem).isEqualTo(DELIUS.name)
    assertThat(loggedEvent.eventTimestamp).isBefore(LocalDateTime.now())
    assertThat(loggedEvent.beforeData).isEqualTo(beforeData)
    assertThat(loggedEvent.processedData).isEqualTo(processedData)
    assertThat(loggedEvent.uuid).isEqualTo(updatedPersonEntity.personKey?.personId.toString())
  }
}
