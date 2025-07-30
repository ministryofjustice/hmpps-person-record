package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
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
import java.lang.Thread.sleep
import java.time.LocalDate
import java.util.stream.Stream

class PrisonEventListenerIntTest : MessagingMultiNodeTestBase() {
  fun waitForMessageToBeProcessedAndDiscarded() {
    sleep(1000)
  }

  @Test
  fun `should put message on dlq when exception thrown`() {
    val prisonNumber = randomPrisonNumber()
    stub5xxResponse("/prisoner/$prisonNumber", currentScenarioState = "next request will fail", nextScenarioState = "next request will fail", scenarioName = "processing fail")
    stub5xxResponse("/prisoner/$prisonNumber", "next request will fail", scenarioName = "processing fail")
    stub5xxResponse("/prisoner/$prisonNumber", "next request will fail", scenarioName = "processing fail")
    val domainEvent = prisonDomainEvent(PRISONER_CREATED, prisonNumber)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    expectOneMessageOnDlq(prisonEventsQueue)
  }

  @Test
  fun `should discard message if prisoner search returns 404`() {
    val prisonNumber = randomPrisonNumber()
    stub404Response("/prisoner/$prisonNumber")
    val domainEvent = prisonDomainEvent(PRISONER_CREATED, prisonNumber)
    publishDomainEvent(PRISONER_CREATED, domainEvent)
    waitForMessageToBeProcessedAndDiscarded()
    expectNoMessagesOnQueueOrDlq(prisonEventsQueue)
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
      val title = "Mr"
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
      stubPrisonResponse(ApiResponseSetup(title = title, gender = "Female", aliases = listOf(ApiResponseSetupAlias(title, aliasFirstName, aliasMiddleName, aliasLastName, aliasDateOfBirth)), firstName = firstName, middleName = middleName, lastName = lastName, prisonNumber = prisonNumber, pnc = pnc, email = email, sentenceStartDate = sentenceStartDate, primarySentence = primarySentence, cro = cro, addresses = listOf(ApiResponseSetupAddress(postcode = postcode, fullAddress = fullAddress, startDate = LocalDate.of(1970, 1, 1), noFixedAbode = true)), dateOfBirth = personDateOfBirth, nationality = nationality, ethnicity = ethnicity, religion = religion, identifiers = listOf(ApiResponseSetupIdentifier(type = "NINO", value = nationalInsuranceNumber), ApiResponseSetupIdentifier(type = "DL", value = driverLicenseNumber))))
      val domainEvent = prisonDomainEvent(PRISONER_CREATED, prisonNumber)
      publishDomainEvent(PRISONER_CREATED, domainEvent)

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(personEntity.personKey).isNotNull()
        assertThat(personEntity.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
        assertThat(personEntity.getPrimaryName().title).isEqualTo("Mr")
        assertThat(personEntity.getPrimaryName().titleCode?.code).isEqualTo("MR")
        assertThat(personEntity.getPrimaryName().titleCode?.description).isEqualTo("Mr")
        assertThat(personEntity.getPrimaryName().firstName).isEqualTo(firstName)
        assertThat(personEntity.getPrimaryName().middleNames).isEqualTo("$middleName $middleName")
        assertThat(personEntity.getPrimaryName().lastName).isEqualTo(lastName)
        assertThat(personEntity.nationality).isEqualTo(nationality)
        assertThat(personEntity.religion).isEqualTo(religion)
        assertThat(personEntity.ethnicity).isEqualTo(ethnicity)
        assertThat(personEntity.getPnc()).isEqualTo(pnc)
        assertThat(personEntity.getCro()).isEqualTo(cro)
        assertThat(personEntity.references.getType(IdentifierType.NATIONAL_INSURANCE_NUMBER).first().identifierValue).isEqualTo(nationalInsuranceNumber)
        assertThat(personEntity.references.getType(IdentifierType.DRIVER_LICENSE_NUMBER).first().identifierValue).isEqualTo(driverLicenseNumber)
        assertThat(personEntity.getPrimaryName().dateOfBirth).isEqualTo(personDateOfBirth)
        assertThat(personEntity.getAliases().size).isEqualTo(1)
        assertThat(personEntity.getAliases()[0].title).isEqualTo("Mr")
        assertThat(personEntity.getAliases()[0].titleCode?.code).isEqualTo("MR")
        assertThat(personEntity.getAliases()[0].titleCode?.description).isEqualTo("Mr")
        assertThat(personEntity.getAliases()[0].firstName).isEqualTo(aliasFirstName)
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
        assertThat(personEntity.sentenceInfo[0].sentenceDate).isEqualTo(sentenceStartDate)
        assertThat(personEntity.sexCode).isEqualTo(SexCode.F)
      }

      checkEventLogExist(prisonNumber, CPRLogEvents.CPR_RECORD_CREATED)

      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber))
    }

    @Test
    fun `should check nationality and religion null`() {
      val prisonNumber = randomPrisonNumber()

      stubNoMatchesPersonMatch()
      stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber, pnc = randomPnc(), email = randomEmail(), cro = randomCro(), addresses = listOf(ApiResponseSetupAddress(postcode = randomPostcode(), fullAddress = randomFullAddress())), dateOfBirth = randomDate(), nationality = null, religion = null))
      val domainEvent = prisonDomainEvent(PRISONER_CREATED, prisonNumber)
      publishDomainEvent(PRISONER_CREATED, domainEvent)

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )

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
      val domainEvent = prisonDomainEvent(PRISONER_CREATED, prisonNumber)
      publishDomainEvent(PRISONER_CREATED, domainEvent)

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(personEntity.sentenceInfo.size).isEqualTo(0)
      }

      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber))
    }

    @Test
    fun `should receive the message successfully when prisoner updated event published`() {
      val prisonNumber = randomPrisonNumber()
      val prisoner = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))

      val updatedFirstName = randomName()

      stubNoMatchesPersonMatch(matchId = prisoner.matchId)
      stubPrisonResponse(ApiResponseSetup(gender = "Male", title = "Mr", prisonNumber = prisonNumber, firstName = updatedFirstName))
      val domainEvent = prisonDomainEvent(PRISONER_UPDATED, prisonNumber)
      publishDomainEvent(PRISONER_UPDATED, domainEvent)

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      checkEventLogExist(prisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber = prisonNumber)!!
        assertThat(personEntity.matchId).isEqualTo(prisoner.matchId)
        assertThat(personEntity.getPrimaryName().title).isEqualTo("Mr")
        assertThat(personEntity.getPrimaryName().titleCode?.code).isEqualTo("MR")
        assertThat(personEntity.getPrimaryName().titleCode?.description).isEqualTo("Mr")
        assertThat(personEntity.getPrimaryName().firstName).isEqualTo(updatedFirstName)
        assertThat(personEntity.sexCode).isEqualTo(SexCode.M)
      }
    }

    @Test
    fun `should not link a new prison record to a cluster if its not above the join threshold`() {
      val prisonNumber = randomPrisonNumber()

      val existingPerson = createPersonWithNewKey(createRandomPrisonPersonDetails(randomPrisonNumber()))

      stubOnePersonMatchAboveFractureThreshold(matchedRecord = existingPerson.matchId)
      stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))

      val domainEvent = prisonDomainEvent(PRISONER_CREATED, prisonNumber)
      publishDomainEvent(PRISONER_CREATED, domainEvent)

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber))
      checkEventLogExist(prisonNumber, CPRLogEvents.CPR_RECORD_CREATED)
      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber))

      awaitNotNullPerson { personRepository.findByPrisonNumber(prisonNumber) }
    }

    @Test
    fun `should retry on retryable 500 error from prisoner search`() {
      val prisonNumber = randomPrisonNumber()
      stubNoMatchesPersonMatch()
      stub5xxResponse("/prisoner/$prisonNumber", nextScenarioState = "next request will succeed", scenarioName = "retry")
      stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber), scenarioName = "retry", currentScenarioState = "next request will succeed")

      val domainEvent = prisonDomainEvent(PRISONER_CREATED, prisonNumber)
      publishDomainEvent(PRISONER_CREATED, domainEvent)

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
    fun `should retry message if person-match returns 404`() {
      val prisonNumber = randomPrisonNumber()
      stubPrisonResponse(ApiResponseSetup(gender = "Male", prisonNumber = prisonNumber))
      stubPersonMatchScores(status = 404, nextScenarioState = "will succeed")

      stubPrisonResponse(ApiResponseSetup(gender = "Male", prisonNumber = prisonNumber), currentScenarioState = "will succeed")
      stubPersonMatchUpsert(currentScenarioState = "will succeed")
      stubPersonMatchScores(currentScenarioState = "will succeed")
      val domainEvent = prisonDomainEvent(PRISONER_CREATED, prisonNumber)
      publishDomainEvent(PRISONER_CREATED, domainEvent)
      awaitNotNullPerson { personRepository.findByPrisonNumber(prisonNumber = prisonNumber) }
      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      expectNoMessagesOnQueueOrDlq(prisonEventsQueue)
    }

    @Test
    fun `should allow a person to be created from a prison event when an offender record already exists with the prisonNumber`() {
      val prisonNumber = randomPrisonNumber()
      stubNoMatchesPersonMatch()
      probationDomainEventAndResponseSetup(eventType = NEW_OFFENDER_CREATED, ApiResponseSetup(pnc = randomPnc(), prisonNumber = prisonNumber, crn = randomCrn()))
      val domainEvent = prisonDomainEvent(PRISONER_CREATED, prisonNumber)
      stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))
      publishDomainEvent(PRISONER_CREATED, domainEvent)
      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to SourceSystemType.NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
    }
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
      stubPrisonResponse(ApiResponseSetup(aliases = listOf(ApiResponseSetupAlias(firstName = aliasFirstName, middleName = aliasMiddleName, lastName = aliasLastName, dateOfBirth = aliasDateOfBirth)), firstName = firstName, middleName = middleName, lastName = lastName, prisonNumber = prisonNumber, pnc = pnc, sentenceStartDate = sentenceStartDate, primarySentence = true, cro = cro, addresses = listOf(ApiResponseSetupAddress(postcode = postcode, startDate = LocalDate.of(1970, 1, 1), noFixedAbode = true, fullAddress = "")), dateOfBirth = personDateOfBirth))
      val domainEvent = prisonDomainEvent(PRISONER_CREATED, prisonNumber)
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
        assertThat(createdLog.personUUID).isNotNull()
        assertThat(createdLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
      }
      checkEventLogExist(prisonNumber, CPRLogEvents.CPR_UUID_CREATED)
    }
  }

  @ParameterizedTest
  @MethodSource("prisonTitleCodes")
  fun `should map all title codes to cpr title codes`(prisonTitleCode: String?, cprTitleCode: String?, cprTitleCodeDescription: String?) {
    val prisonNumber = randomPrisonNumber()
    stubNoMatchesPersonMatch()
    stubPersonMatchUpsert()
    stubPrisonResponse(ApiResponseSetup(title = prisonTitleCode, prisonNumber = prisonNumber))
    publishDomainEvent(PRISONER_CREATED, prisonDomainEvent(PRISONER_CREATED, prisonNumber))
    val person = awaitNotNullPerson { personRepository.findByPrisonNumber(prisonNumber) }
    assertThat(person.getPrimaryName().title).isEqualTo(prisonTitleCode)
    assertThat(person.getPrimaryName().titleCode?.code).isEqualTo(cprTitleCode)
    assertThat(person.getPrimaryName().titleCode?.description).isEqualTo(cprTitleCodeDescription)
  }

  companion object {

    @JvmStatic
    fun prisonTitleCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Mr", "MR", "Mr"),
      Arguments.of("Mrs", "MRS", "Mrs"),
      Arguments.of("Miss", "MISS", "Miss"),
      Arguments.of("Ms", "MS", "Ms"),
      Arguments.of("Reverend", "REV", "Reverend"),
      Arguments.of("Father", "FR", "Father"),
      Arguments.of("Imam", "IMAM", "Imam"),
      Arguments.of("Rabbi", "RABBI", "Rabbi"),
      Arguments.of("Brother", "BR", "Brother"),
      Arguments.of("Sister", "SR", "Sister"),
      Arguments.of("Dame", "DME", "Dame"),
      Arguments.of("Dr", "DR", "Dr"),
      Arguments.of("Lady", "LDY", "Lady"),
      Arguments.of("Lord", "LRD", "Lord"),
      Arguments.of("Sir", "SIR", "Sir"),
      Arguments.of("Invalid", "UN", "Unknown"),
    )
  }
}
