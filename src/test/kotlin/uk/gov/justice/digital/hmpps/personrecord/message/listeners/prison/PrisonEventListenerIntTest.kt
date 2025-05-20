package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
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
import java.util.stream.Stream

class PrisonEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should put message on dlq when exception thrown`() {
    val prisonNumber = randomPrisonNumber()
    stub5xxResponse("/prisoner/$prisonNumber", currentScenarioState = "next request will fail", nextScenarioState = "next request will fail", scenarioName = "processing fail")
    stub5xxResponse("/prisoner/$prisonNumber", "next request will fail", scenarioName = "processing fail")
    stub5xxResponse("/prisoner/$prisonNumber", "next request will fail", scenarioName = "processing fail")
    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name))

    expectOneMessageOnDlq(prisonEventsQueue)
  }

  @ParameterizedTest
  @MethodSource("currentlyManagedParameters")
  fun `should map currently managed field correctly`(status: String?, result: Boolean?) {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
    val prisonNumber = randomPrisonNumber()
    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber, currentlyManaged = status))

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber)
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)

    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
    )

    val personEntity = awaitNotNullPerson { personRepository.findByPrisonNumber(prisonNumber) }
    assertThat(personEntity.currentlyManaged).isEqualTo(result)
  }

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
    }

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

      stubNoMatchesPersonMatch()
      stubPrisonResponse(ApiResponseSetup(gender = "Female", aliases = listOf(ApiResponseSetupAlias(aliasFirstName, aliasMiddleName, aliasLastName, aliasDateOfBirth)), firstName = firstName, middleName = middleName, lastName = lastName, prisonNumber = prisonNumber, pnc = pnc, email = email, sentenceStartDate = sentenceStartDate, primarySentence = primarySentence, cro = cro, addresses = listOf(ApiResponseSetupAddress(postcode = postcode, fullAddress = fullAddress, startDate = LocalDate.of(1970, 1, 1), noFixedAbode = true)), dateOfBirth = personDateOfBirth, nationality = nationality, ethnicity = ethnicity, religion = religion, currentlyManaged = "ACTIVE IN", identifiers = listOf(ApiResponseSetupIdentifier(type = "NINO", value = nationalInsuranceNumber), ApiResponseSetupIdentifier(type = "DL", value = driverLicenseNumber))))

      val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
      val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
      publishDomainEvent(PRISONER_CREATED, domainEvent)

      checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name))

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(personEntity.personKey).isNotNull()
        assertThat(personEntity.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
        assertThat(personEntity.getPrimaryName().title).isEqualTo("Ms")
        assertThat(personEntity.getPrimaryName().firstName).isEqualTo(firstName)
        assertThat(personEntity.getPrimaryName().middleNames).isEqualTo("$middleName $middleName")
        assertThat(personEntity.getPrimaryName().lastName).isEqualTo(lastName)
        assertThat(personEntity.nationality).isEqualTo(nationality)
        assertThat(personEntity.religion).isEqualTo(religion)
        assertThat(personEntity.ethnicity).isEqualTo(ethnicity)
        assertThat(personEntity.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
        assertThat(personEntity.references.getType(IdentifierType.CRO).first().identifierValue).isEqualTo(cro)
        assertThat(personEntity.references.getType(IdentifierType.NATIONAL_INSURANCE_NUMBER).first().identifierValue).isEqualTo(nationalInsuranceNumber)
        assertThat(personEntity.references.getType(IdentifierType.DRIVER_LICENSE_NUMBER).first().identifierValue).isEqualTo(driverLicenseNumber)
        assertThat(personEntity.getPrimaryName().dateOfBirth).isEqualTo(personDateOfBirth)
        assertThat(personEntity.getAliases().size).isEqualTo(1)
        assertThat(personEntity.getAliases()[0].firstName).isEqualTo(aliasFirstName)
        assertThat(personEntity.getAliases()[0].title).isEqualTo("AliasTitle")
        assertThat(personEntity.getAliases()[0].middleNames).isEqualTo(aliasMiddleName)
        assertThat(personEntity.getAliases()[0].lastName).isEqualTo(aliasLastName)
        assertThat(personEntity.getAliases()[0].dateOfBirth).isEqualTo(aliasDateOfBirth)
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
        assertThat(personEntity.sexCode).isEqualTo(SexCode.F)
      }

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      checkEventLogExist(prisonNumber, CPRLogEvents.CPR_RECORD_CREATED)

      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber))
    }

    @Test
    fun `should check nationality and religion null`() {
      val prisonNumber = randomPrisonNumber()

      stubNoMatchesPersonMatch()
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

      stubNoMatchesPersonMatch()
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
    fun `should receive the message successfully when prisoner updated event published`() {
      val prisoner = createPrisoner()

      val prisonNumber = prisoner.prisonNumber!!
      val updatedFirstName = randomName()

      stubNoMatchesPersonMatch(matchId = prisoner.matchId)
      stubPrisonResponse(ApiResponseSetup(gender = "Male", prisonNumber = prisonNumber, firstName = updatedFirstName))

      val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = listOf("SENTENCE"))
      val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, personReference = null, additionalInformation = additionalInformation)
      publishDomainEvent(PRISONER_UPDATED, domainEvent)

      checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_UPDATED, "SOURCE_SYSTEM" to SourceSystemType.NOMIS.name))

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      checkEventLogExist(prisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber = prisonNumber)!!
        assertThat(personEntity.matchId).isEqualTo(prisoner.matchId)
        assertThat(personEntity.getPrimaryName().firstName).isEqualTo(updatedFirstName)
        assertThat(personEntity.sexCode).isEqualTo(SexCode.M)
      }
    }

    @Test
    fun `should retry on retryable error`() {
      val prisonNumber = randomPrisonNumber()
      stubNoMatchesPersonMatch()
      stub5xxResponse("/prisoner/$prisonNumber", nextScenarioState = "next request will succeed", scenarioName = "retry")
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
    fun `should allow a person to be created from a prison event when an offender record already exists with the prisonNumber`() {
      val prisonNumber = randomPrisonNumber()
      stubNoMatchesPersonMatch()
      probationDomainEventAndResponseSetup(eventType = NEW_OFFENDER_CREATED, ApiResponseSetup(pnc = randomPnc(), prisonNumber = prisonNumber, crn = randomCrn()))
      val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
      val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)

      stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))
      publishDomainEvent(PRISONER_CREATED, domainEvent)
      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
    }

    private fun createPrisoner(): PersonEntity = createPersonWithNewKey(
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
    )
  }

  @Nested
  inner class EventLog {

    @Test
    fun `should save record details in event log on create`() {
      val prisonNumber = randomPrisonNumber()
      val firstName = randomName()
      val middleName = randomName()
      val lastName = randomName()
      val pnc = randomPnc()
      val cro = randomCro()
      val postcode = randomPostcode()
      val personDateOfBirth = randomDate()
      val sentenceStartDate = randomDate()
      val aliasFirstName = randomName()
      val aliasMiddleName = randomName()
      val aliasLastName = randomName()
      val aliasDateOfBirth = randomDate()

      stubPersonMatchUpsert()
      stubNoMatchesPersonMatch()
      stubPrisonResponse(ApiResponseSetup(aliases = listOf(ApiResponseSetupAlias(aliasFirstName, aliasMiddleName, aliasLastName, aliasDateOfBirth)), firstName = firstName, middleName = middleName, lastName = lastName, prisonNumber = prisonNumber, pnc = pnc, sentenceStartDate = sentenceStartDate, primarySentence = true, cro = cro, addresses = listOf(ApiResponseSetupAddress(postcode = postcode, startDate = LocalDate.of(1970, 1, 1), noFixedAbode = true, fullAddress = "")), dateOfBirth = personDateOfBirth))

      val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
      val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
      publishDomainEvent(PRISONER_CREATED, domainEvent)

      checkEventLog(prisonNumber, CPRLogEvents.CPR_RECORD_CREATED) { eventLogs ->
        assertThat(eventLogs.size).isEqualTo(1)
        val createdLog = eventLogs.first()
        assertThat(createdLog.pncs).isEqualTo(arrayOf(pnc))
        assertThat(createdLog.cros).isEqualTo(arrayOf(cro))
        assertThat(createdLog.firstName).isEqualTo(firstName)
        assertThat(createdLog.middleNames).isEqualTo("$middleName $middleName")
        assertThat(createdLog.lastName).isEqualTo(lastName)
        assertThat(createdLog.dateOfBirth).isEqualTo(personDateOfBirth)
        assertThat(createdLog.sourceSystem).isEqualTo(SourceSystemType.NOMIS)
        assertThat(createdLog.postcodes).isEqualTo(arrayOf(postcode))
        assertThat(createdLog.sentenceDates).isEqualTo(arrayOf(sentenceStartDate))
        assertThat(createdLog.firstNameAliases).isEqualTo(arrayOf(aliasFirstName))
        assertThat(createdLog.lastNameAliases).isEqualTo(arrayOf(aliasLastName))
        assertThat(createdLog.dateOfBirthAliases).isEqualTo(arrayOf(aliasDateOfBirth))
        assertThat(createdLog.uuid).isNotNull()
        assertThat(createdLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
      }
      checkEventLogExist(prisonNumber, CPRLogEvents.CPR_UUID_CREATED)
    }
  }

  companion object {
    @JvmStatic
    fun currentlyManagedParameters(): Stream<Arguments> = Stream.of(
      Arguments.of("ACTIVE IN", true),
      Arguments.of("ACTIVE OUT", true),
      Arguments.of("INACTIVE TRN", true),
      Arguments.of("INACTIVE OUT", false),
      Arguments.of("INACTIVE IN", null),
      Arguments.of(null, null),
    )
  }
}
