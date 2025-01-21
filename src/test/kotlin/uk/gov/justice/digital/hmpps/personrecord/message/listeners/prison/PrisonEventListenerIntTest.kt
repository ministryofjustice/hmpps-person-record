package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.AllConvictedOffences
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.EmailAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Identifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.EMAIL
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_NEW_RECORD_EXISTS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UPDATE_RECORD_DOES_NOT_EXIST
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationality
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupIdentifier
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream

class PrisonEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should receive the message successfully when prisoner created event published`() {
    val prisonNumber = randomPrisonNumber()
    val firstName = randomName()
    val middleName = randomName()
    val lastName = randomName()
    val pnc = randomPnc()
    val email = randomEmail()
    val cro = randomCro()
    val postcode = randomPostcode()
    val fullAddress = randomFullAddress()
    val nationality = randomNationality()
    val religion = randomReligion()
    val personDateOfBirth = randomDate()
    val nationalInsuranceNumber = randomNationalInsuranceNumber()
    val driverLicenseNumber = randomDriverLicenseNumber()
    val ethnicity = randomEthnicity()
    val sentenceStartDate = randomDate()
    val primarySentence = true
    val aliasFirstName = randomName()
    val aliasMiddleName = randomName()
    val aliasLastName = randomName()
    val aliasDateOfBirth = randomDate()

    stubPrisonResponse(ApiResponseSetup(aliases = listOf(ApiResponseSetupAlias(aliasFirstName, aliasMiddleName, aliasLastName, aliasDateOfBirth)), firstName = firstName, middleName = middleName, lastName = lastName, prisonNumber = prisonNumber, pnc = pnc, email = email, sentenceStartDate = sentenceStartDate, primarySentence = primarySentence, cro = cro, addresses = listOf(ApiResponseSetupAddress(postcode = postcode, fullAddress = fullAddress, startDate = LocalDate.of(1970, 1, 1), noFixedAbode = true)), dateOfBirth = personDateOfBirth, nationality = nationality, ethnicity = ethnicity, religion = religion, currentlyManaged = "ACTIVE IN", identifiers = listOf(ApiResponseSetupIdentifier(type = "NINO", value = nationalInsuranceNumber), ApiResponseSetupIdentifier(type = "DL", value = driverLicenseNumber))))

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name))

    awaitAssert {
      val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(personEntity.personKey).isNotNull()
      assertThat(personEntity.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
      assertThat(personEntity.title).isEqualTo("Ms")
      assertThat(personEntity.firstName).isEqualTo(firstName)
      assertThat(personEntity.middleNames).isEqualTo("$middleName $middleName")
      assertThat(personEntity.lastName).isEqualTo(lastName)
      assertThat(personEntity.nationality).isEqualTo(nationality)
      assertThat(personEntity.religion).isEqualTo(religion)
      assertThat(personEntity.ethnicity).isEqualTo(ethnicity)
      assertThat(personEntity.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
      assertThat(personEntity.references.getType(IdentifierType.CRO).first().identifierValue).isEqualTo(cro)
      assertThat(personEntity.references.getType(IdentifierType.NATIONAL_INSURANCE_NUMBER).first().identifierValue).isEqualTo(nationalInsuranceNumber)
      assertThat(personEntity.references.getType(IdentifierType.DRIVER_LICENSE_NUMBER).first().identifierValue).isEqualTo(driverLicenseNumber)
      assertThat(personEntity.dateOfBirth).isEqualTo(personDateOfBirth)
      assertThat(personEntity.pseudonyms.size).isEqualTo(1)
      assertThat(personEntity.pseudonyms[0].firstName).isEqualTo(aliasFirstName)
      assertThat(personEntity.pseudonyms[0].title).isEqualTo("AliasTitle")
      assertThat(personEntity.pseudonyms[0].middleNames).isEqualTo(aliasMiddleName)
      assertThat(personEntity.pseudonyms[0].lastName).isEqualTo(aliasLastName)
      assertThat(personEntity.pseudonyms[0].dateOfBirth).isEqualTo(aliasDateOfBirth)
      assertThat(personEntity.addresses.size).isEqualTo(1)
      assertThat(personEntity.addresses[0].postcode).isEqualTo(postcode)
      assertThat(personEntity.addresses[0].fullAddress).isEqualTo(fullAddress)
      assertThat(personEntity.addresses[0].startDate).isEqualTo(LocalDate.of(1970, 1, 1))
      assertThat(personEntity.addresses[0].noFixedAbode).isEqualTo(true)
      assertThat(personEntity.contacts.size).isEqualTo(3)
      assertThat(personEntity.contacts[0].contactType).isEqualTo(EMAIL)
      assertThat(personEntity.contacts[0].contactValue).isEqualTo(email)
      assertThat(personEntity.contacts[1].contactType).isEqualTo(HOME)
      assertThat(personEntity.contacts[1].contactValue).isEqualTo("01141234567")
      assertThat(personEntity.contacts[2].contactType).isEqualTo(MOBILE)
      assertThat(personEntity.contacts[2].contactValue).isEqualTo("01141234567")
      assertThat(personEntity.currentlyManaged).isEqualTo(true)
      assertThat(personEntity.sentenceInfo[0].sentenceDate).isEqualTo(sentenceStartDate)
    }

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
    )
    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber))
  }

  @Test
  fun `should check nationality and religion null`() {
    val prisonNumber = randomPrisonNumber()

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber, pnc = randomPnc(), email = randomEmail(), cro = randomCro(), addresses = listOf(ApiResponseSetupAddress(postcode = randomPostcode(), fullAddress = randomFullAddress())), dateOfBirth = randomDate(), nationality = null, religion = null))

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name))

    awaitAssert {
      val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!

      assertThat(personEntity.nationality).isEqualTo(null)
      assertThat(personEntity.religion).isEqualTo(null)
    }

    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber))
  }

  @Test
  fun `should check sentence start date is not populated when primary sentence is false`() {
    val prisonNumber = randomPrisonNumber()
    val sentenceStartDate = randomDate()

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber, pnc = randomPnc(), sentenceStartDate = sentenceStartDate, primarySentence = false, email = randomEmail(), cro = randomCro(), addresses = listOf(ApiResponseSetupAddress(postcode = randomPostcode(), fullAddress = randomFullAddress())), dateOfBirth = randomDate(), nationality = null, religion = null))

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name))

    awaitAssert {
      val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(personEntity.sentenceInfo.size).isEqualTo(0)
    }

    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber))
  }

  @Test
  fun `should log correct telemetry on created event but record already exists`() {
    val prisoner = createPrisoner()

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisoner.prisonNumber))

    val additionalInformation = AdditionalInformation(prisonNumber = prisoner.prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisoner.prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name))
    checkTelemetry(CPR_NEW_RECORD_EXISTS, mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisoner.prisonNumber))
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisoner.prisonNumber),
    )
    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to prisoner.personKey?.personId.toString()),
    )
  }

  @Test
  fun `should receive the message successfully when prisoner updated event published`() {
    val prisoner = createPrisoner()

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisoner.prisonNumber))

    val additionalInformation = AdditionalInformation(prisonNumber = prisoner.prisonNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisoner.prisonNumber, "EVENT_TYPE" to PRISONER_UPDATED, "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name))

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisoner.prisonNumber),
    )

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to prisoner.personKey?.personId.toString()),
    )
  }

  @Test
  fun `should retry on retryable error`() {
    val prisonNumber = randomPrisonNumber()
    stub500Response(prisonNumber, "next request will succeed")
    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber), scenarioName = "retry", currentScenarioState = "next request will succeed")
    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name))

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
    )
    checkTelemetry(
      CPR_UUID_CREATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
    )
  }

  @Test
  fun `should create message processing failed telemetry event when exception thrown`() {
    val prisonNumber = randomPrisonNumber()
    stub500Response(prisonNumber, STARTED)
    stub500Response(prisonNumber, STARTED)
    stub500Response(prisonNumber, STARTED)
    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    val messageId = publishDomainEvent(PRISONER_CREATED, domainEvent)

    purgeQueueAndDlq(prisonEventsQueue)
    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name))

    checkTelemetry(
      TelemetryEventType.MESSAGE_PROCESSING_FAILED,
      mapOf(
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name,
        "EVENT_TYPE" to "prisoner-offender-search.prisoner.created",
      ),
    )
  }

  @Test
  fun `should log correct telemetry on updated event but no record exists`() {
    val prisonNumber = randomPrisonNumber()
    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_UPDATED, "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name))
    checkTelemetry(CPR_UPDATE_RECORD_DOES_NOT_EXIST, mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber))
    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
    )
    checkTelemetry(
      CPR_UUID_CREATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
    )
  }

  @Test
  fun `should allow a person to be created from a prison event when an offender record already exists with the prisonNumber`() {
    val prisonNumber = randomPrisonNumber()
    probationDomainEventAndResponseSetup(eventType = OFFENDER_ALIAS_CHANGED, ApiResponseSetup(pnc = randomPnc(), prisonNumber = prisonNumber, crn = randomCrn()))
    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))
    publishDomainEvent(PRISONER_UPDATED, domainEvent)
    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
    )
  }

  @ParameterizedTest
  @MethodSource("currentlyManagedParameters")
  fun `should map currently managed field correctly`(status: String?, result: Boolean?) {
    val prisonNumber = randomPrisonNumber()
    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber, currentlyManaged = status))

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber)
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    stubOneHighConfidenceMatch()
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
    )

    val personEntity = awaitNotNullPerson { personRepository.findByPrisonNumber(prisonNumber) }
    assertThat(personEntity.currentlyManaged).isEqualTo(result)
  }

  @Test
  fun `should map to EventLogging table when prisoner create and update event`() {
    val prisoner = createPrisoner()

    val personEntity = personRepository.findByPrisonNumber(prisoner.prisonNumber!!)
    val beforeDataTO = personEntity?.let { Person.from(it) }
    val beforeData = objectMapper.writeValueAsString(beforeDataTO)

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisoner.prisonNumber))

    val additionalInformation = AdditionalInformation(prisonNumber = prisoner.prisonNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)

    val updatedPersonEntity = awaitNotNullPerson { personRepository.findByPrisonNumber(prisoner.prisonNumber!!) }

    val processedDTO = Person.from(updatedPersonEntity)
    val processedData = objectMapper.writeValueAsString(processedDTO)

    val loggedEvent = awaitNotNullEventLog(prisoner.prisonNumber!!, PRISONER_UPDATED)

    assertThat(loggedEvent.sourceSystemId).isEqualTo(prisoner.prisonNumber)
    assertThat(loggedEvent.sourceSystem).isEqualTo(SourceSystemType.NOMIS.name)
    assertThat(loggedEvent.eventTimestamp).isBefore(LocalDateTime.now())
    assertThat(loggedEvent.beforeData).isEqualTo(beforeData)
    assertThat(loggedEvent.processedData).isEqualTo(processedData)

    assertThat(loggedEvent.uuid).isEqualTo(personEntity?.personKey?.personId.toString())
  }

  private fun createPrisoner(): PersonEntity = createPerson(
    Person.from(
      Prisoner(
        prisonNumber = randomPrisonNumber(),
        title = "Ms",
        firstName = randomName(),
        middleNames = "${randomName()} ${randomName()}",
        lastName = randomName(),
        cro = CROIdentifier.from(randomCro()),
        pnc = PNCIdentifier.from(randomPnc()),
        dateOfBirth = randomDate(),
        emailAddresses = listOf(EmailAddress(randomEmail())),
        nationality = randomNationality(),
        religion = randomReligion(),
        identifiers = listOf(Identifier(type = "NINO", randomNationalInsuranceNumber()), Identifier(type = "DL", randomDriverLicenseNumber())),
        ethnicity = randomEthnicity(),
        allConvictedOffences = listOf(AllConvictedOffences(randomDate())),
      ),
    ),
    personKeyEntity = createPersonKey(),
  )

  private fun stub500Response(
    prisonNumber: String,
    nextScenarioState: String,
  ) {
    wiremock.stubFor(
      WireMock.get("/prisoner/$prisonNumber")
        .inScenario("retry")
        .whenScenarioStateIs(STARTED)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  companion object {
    @JvmStatic
    private fun currentlyManagedParameters(): Stream<Arguments> = Stream.of(
      Arguments.of("ACTIVE IN", true),
      Arguments.of("ACTIVE OUT", true),
      Arguments.of("INACTIVE TRN", true),
      Arguments.of("INACTIVE OUT", false),
      Arguments.of("INACTIVE IN", null),
      Arguments.of(null, null),
    )
  }
}
