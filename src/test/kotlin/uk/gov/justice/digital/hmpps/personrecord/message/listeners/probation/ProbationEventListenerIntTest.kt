package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationality
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupSentences
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit.SECONDS

class ProbationEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Nested
  inner class SuccessfulProcessing {
    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
    }

    @Test
    fun `creates person when when new offender created event is published`() {
      val crn = randomCrn()
      val title = randomName()
      val prisonNumber = randomPrisonNumber()
      val firstName = randomName()
      val middleName = randomName()
      val lastName = randomName()
      val pnc = randomPnc()
      val cro = randomCro()
      val addressStartDate = randomDate()
      val addressEndDate = randomDate()
      val ethnicity = randomEthnicity()
      val nationality = randomNationality()
      val sentenceDate = randomDate()
      val aliasFirstName = randomName()
      val aliasMiddleName = randomName()
      val aliasLastName = randomName()
      val aliasDateOfBirth = randomDate()

      val apiResponse = ApiResponseSetup(
        crn = crn,
        pnc = pnc,
        title = title,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        prisonNumber = prisonNumber,
        cro = cro,
        addresses = listOf(
          ApiResponseSetupAddress(noFixedAbode = true, addressStartDate, addressEndDate, postcode = "LS1 1AB", fullAddress = "abc street"),
          ApiResponseSetupAddress(postcode = "M21 9LX", fullAddress = "abc street"),
        ),
        aliases = listOf(ApiResponseSetupAlias(aliasFirstName, aliasMiddleName, aliasLastName, aliasDateOfBirth)),
        ethnicity = ethnicity,
        nationality = nationality,
        sentences = listOf(ApiResponseSetupSentences(sentenceDate)),
      )
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, apiResponse)

      val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }

      assertThat(personEntity.personKey).isNotNull()
      assertThat(personEntity.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
      assertThat(personEntity.firstName).isEqualTo(firstName)
      assertThat(personEntity.middleNames).isEqualTo(middleName)
      assertThat(personEntity.lastName).isEqualTo(lastName)
      assertThat(personEntity.title).isEqualTo(title)
      assertThat(personEntity.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
      assertThat(personEntity.crn).isEqualTo(crn)
      assertThat(personEntity.ethnicity).isEqualTo(ethnicity)
      assertThat(personEntity.nationality).isEqualTo(nationality)
      assertThat(personEntity.sentenceInfo[0].sentenceDate).isEqualTo(sentenceDate)
      assertThat(personEntity.references.getType(IdentifierType.CRO).first().identifierValue).isEqualTo(cro)
      assertThat(personEntity.pseudonyms.size).isEqualTo(1)
      assertThat(personEntity.pseudonyms[0].firstName).isEqualTo(aliasFirstName)
      assertThat(personEntity.pseudonyms[0].middleNames).isEqualTo(aliasMiddleName)
      assertThat(personEntity.pseudonyms[0].lastName).isEqualTo(aliasLastName)
      assertThat(personEntity.pseudonyms[0].dateOfBirth).isEqualTo(aliasDateOfBirth)
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
      assertThat(personEntity.matchId).isNotNull()
      assertThat(personEntity.lastModified).isNotNull()

      checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    }

    @Test
    fun `should link new probation record to an existing prison record`() {
      telemetryRepository.deleteAll()
      val crn = randomCrn()
      val prisonNumber = randomPrisonNumber()
      val firstName = randomName()
      val pnc = randomPnc()
      val cro = randomCro()
      val postcode = randomPostcode()
      val existingPrisoner = Person(
        prisonNumber = prisonNumber,
        references = listOf(
          Reference(identifierType = IdentifierType.PNC, identifierValue = pnc),
          Reference(identifierType = IdentifierType.CRO, identifierValue = cro),
        ),
        addresses = listOf(Address(postcode = postcode)),
        sourceSystem = NOMIS,
      )
      val personKeyEntity = createPersonKey()
      createPerson(existingPrisoner, personKeyEntity = personKeyEntity)

      stubOneHighConfidenceMatch()

      val apiResponse = ApiResponseSetup(
        crn = crn,
        pnc = pnc,
        firstName = firstName,
        prisonNumber = prisonNumber,
        cro = cro,
        addresses = listOf(ApiResponseSetupAddress(postcode = postcode, fullAddress = "abc street"), ApiResponseSetupAddress(postcode = randomPostcode(), fullAddress = "abc street")),
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
      val crn = randomCrn()
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
      val crn = randomCrn()
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, prisonNumber = prisonNumber))
      awaitNotNullPerson { personRepository.findByCrn(crn) }
      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val nextCrn = randomCrn()
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = nextCrn, prisonNumber = prisonNumber))
      awaitNotNullPerson { personRepository.findByCrn(nextCrn) }

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to nextCrn))
      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    }

    @Test
    fun `should handle new offender details with an empty pnc`() {
      val crn = randomCrn()
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = ""))

      val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }

      assertThat(personEntity.references.getType(IdentifierType.PNC)).isEqualTo(emptyList<ReferenceEntity>())

      checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    }

    @Test
    fun `should retry on 500 error`() {
      val crn = randomCrn()
      stub5xxResponse(probationUrl(crn), "next request will succeed", "retry")
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = randomPnc()), scenario = "retry", currentScenarioState = "next request will succeed")

      expectNoMessagesOnQueueOrDlq(probationEventsQueue)

      checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"), 1)
      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    }

    @ParameterizedTest
    @ValueSource(strings = [OFFENDER_DETAILS_CHANGED, OFFENDER_ALIAS_CHANGED, OFFENDER_ADDRESS_CHANGED])
    fun `should process probation events successfully`(event: String) {
      val pnc = randomPnc()
      val crn = randomCrn()
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = pnc))
      val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }
      assertThat(personEntity.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
      checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val createdLastModified = personEntity.lastModified
      val changedPnc = randomPnc()
      probationEventAndResponseSetup(event, ApiResponseSetup(crn = crn, pnc = changedPnc))
      checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to event, "SOURCE_SYSTEM" to "DELIUS"))
      checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val updatedPersonEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }
      assertThat(updatedPersonEntity.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(changedPnc)

      val updatedLastModified = updatedPersonEntity.lastModified

      checkTelemetry(
        CPR_RECLUSTER_MESSAGE_RECEIVED,
        mapOf("UUID" to personEntity.personKey?.personId.toString()),
      )

      assertThat(updatedLastModified).isAfter(createdLastModified)
    }

    @Test
    fun `should log event to EventLogging table when new offender created`() {
      val crn = randomCrn()
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
      val crn = randomCrn()

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

  @Test
  fun `should not push 404 to dead letter queue but discard message instead`() {
    val crn = randomCrn()
    stub404Response(probationUrl(crn))

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, personReference = personReference, additionalInformation = null)
    publishDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    expectNoMessagesOnQueueOrDlq(probationEventsQueue)
    checkTelemetry(MESSAGE_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"), 1)
  }

  @Test
  fun `should log when message processing fails`() {
    val crn = randomCrn()
    stub5xxResponse(probationUrl(crn), nextScenarioState = "request will fail", "failure")
    stub5xxResponse(probationUrl(crn), currentScenarioState = "request will fail", nextScenarioState = "request will fail", scenarioName = "failure")
    stub5xxResponse(probationUrl(crn), currentScenarioState = "request will fail", nextScenarioState = "request will fail", scenarioName = "failure")
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))

    val domainEvent = DomainEvent(
      eventType = NEW_OFFENDER_CREATED,
      personReference = personReference,
      additionalInformation = null,
    )
    val messageId = publishDomainEvent(NEW_OFFENDER_CREATED, domainEvent)
    purgeQueueAndDlq(probationEventsQueue)

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
}
