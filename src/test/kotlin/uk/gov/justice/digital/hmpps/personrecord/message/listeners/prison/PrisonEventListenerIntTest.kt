package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.getEmail
import uk.gov.justice.digital.hmpps.personrecord.extensions.getHome
import uk.gov.justice.digital.hmpps.personrecord.extensions.getMobile
import uk.gov.justice.digital.hmpps.personrecord.extensions.getType
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import uk.gov.justice.digital.hmpps.personrecord.test.randomShortPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitle
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupIdentifier
import java.lang.Thread.sleep
import java.time.LocalDate

class PrisonEventListenerIntTest : MessagingMultiNodeTestBase() {

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
      val title = randomTitle()
      val firstName = randomName()
      val middleName = randomName()
      val lastName = randomName()
      val pnc = randomShortPnc()
      val email = randomEmail()
      val cro = randomCro()
      val postcode = randomPostcode()
      val fullAddress = randomFullAddress()
      val nationality = randomPrisonNationalityCode()
      val religion = randomReligion()
      val personDateOfBirth = randomDate()
      val nationalInsuranceNumber = randomNationalInsuranceNumber()
      val driverLicenseNumber = randomDriverLicenseNumber()
      val ethnicity = randomPrisonEthnicity()
      val sentenceStartDate = randomDate()
      val primarySentence = true
      val aliasFirstName = randomName()
      val aliasMiddleName = randomName()
      val aliasLastName = randomName()
      val aliasDateOfBirth = randomDate()
      val aliasGender = randomPrisonSexCode()
      val gender = randomPrisonSexCode()

      stubNoMatchesPersonMatch()
      prisonDomainEventAndResponseSetup(
        PRISONER_CREATED,
        apiResponseSetup = ApiResponseSetup(title = title, gender = gender.key, aliases = listOf(ApiResponseSetupAlias(title, aliasFirstName, aliasMiddleName, aliasLastName, aliasDateOfBirth, aliasGender.key)), firstName = firstName, middleName = middleName, lastName = lastName, prisonNumber = prisonNumber, pnc = pnc, email = email, sentenceStartDate = sentenceStartDate, primarySentence = primarySentence, cro = cro, addresses = listOf(ApiResponseSetupAddress(postcode = postcode, fullAddress = fullAddress, startDate = LocalDate.of(1970, 1, 1), noFixedAbode = true)), dateOfBirth = personDateOfBirth, nationality = nationality, ethnicity = ethnicity, religion = religion, identifiers = listOf(ApiResponseSetupIdentifier(type = "NINO", value = nationalInsuranceNumber), ApiResponseSetupIdentifier(type = "DL", value = driverLicenseNumber))),
      )

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(personEntity.personKey).isNotNull()
        assertThat(personEntity.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
        val storedTitle = title.getTitle()
        assertThat(personEntity.getPrimaryName().titleCodeLegacy?.code).isEqualTo(storedTitle.code)
        assertThat(personEntity.getPrimaryName().titleCodeLegacy?.description).isEqualTo(storedTitle.description)
        assertThat(personEntity.getPrimaryName().titleCode).isEqualTo(TitleCode.from(title))
        assertThat(personEntity.getPrimaryName().firstName).isEqualTo(firstName)
        assertThat(personEntity.getPrimaryName().middleNames).isEqualTo("$middleName $middleName")
        assertThat(personEntity.getPrimaryName().lastName).isEqualTo(lastName)
        assertThat(personEntity.getPrimaryName().sexCode).isEqualTo(gender.value)
        assertThat(personEntity.religion).isEqualTo(religion)
        assertThat(personEntity.getPnc()).isEqualTo(PNCIdentifier.from(pnc).pncId)
        assertThat(personEntity.getCro()).isEqualTo(cro)
        assertThat(personEntity.references.getType(NATIONAL_INSURANCE_NUMBER).first()).isEqualTo(nationalInsuranceNumber)
        assertThat(personEntity.references.getType(DRIVER_LICENSE_NUMBER).first()).isEqualTo(driverLicenseNumber)
        assertThat(personEntity.getPrimaryName().dateOfBirth).isEqualTo(personDateOfBirth)
        assertThat(personEntity.getAliases().size).isEqualTo(1)
        assertThat(personEntity.getAliases()[0].titleCodeLegacy?.code).isEqualTo(storedTitle.code)
        assertThat(personEntity.getAliases()[0].titleCodeLegacy?.description).isEqualTo(storedTitle.description)
        assertThat(personEntity.getAliases()[0].titleCode).isEqualTo(TitleCode.from(title))
        assertThat(personEntity.getAliases()[0].firstName).isEqualTo(aliasFirstName)
        assertThat(personEntity.getAliases()[0].middleNames).isEqualTo(aliasMiddleName)
        assertThat(personEntity.getAliases()[0].lastName).isEqualTo(aliasLastName)
        assertThat(personEntity.getAliases()[0].dateOfBirth).isEqualTo(aliasDateOfBirth)
        assertThat(personEntity.getAliases()[0].sexCode).isEqualTo(aliasGender.value)
        assertThat(personEntity.addresses.size).isEqualTo(1)
        assertThat(personEntity.addresses[0].postcode).isEqualTo(postcode)
        assertThat(personEntity.addresses[0].fullAddress).isEqualTo(fullAddress)
        assertThat(personEntity.addresses[0].startDate).isEqualTo(LocalDate.of(1970, 1, 1))
        assertThat(personEntity.addresses[0].noFixedAbode).isEqualTo(true)
        assertThat(personEntity.contacts.size).isEqualTo(3)
        assertThat(personEntity.contacts.getEmail()?.contactValue).isEqualTo(email)
        assertThat(personEntity.contacts.getHome()?.contactValue).isEqualTo("01141234567")
        assertThat(personEntity.contacts.getMobile()?.contactValue).isEqualTo("01141234567")
        assertThat(personEntity.sentenceInfo[0].sentenceDate).isEqualTo(sentenceStartDate)
        assertThat(personEntity.nationalities.size).isEqualTo(1)
        assertThat(personEntity.nationalities.first().nationalityCode?.name).isEqualTo(NationalityCode.fromPrisonMapping(nationality)?.name)
        assertThat(personEntity.nationalities.first().nationalityCode?.description).isEqualTo(NationalityCode.fromPrisonMapping(nationality)?.description)

        val storedPrisonEthnicity = ethnicity.getPrisonEthnicity()
        assertThat(personEntity.ethnicityCodeLegacy?.code).isEqualTo(storedPrisonEthnicity.code)
        assertThat(personEntity.ethnicityCodeLegacy?.description).isEqualTo(storedPrisonEthnicity.description)

        assertThat(personEntity.ethnicityCode).isEqualTo(EthnicityCode.fromPrison(ethnicity))
      }

      checkEventLogExist(prisonNumber, CPRLogEvents.CPR_RECORD_CREATED)

      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber))
    }

    @Test
    fun `should check nationality and religion null`() {
      val prisonNumber = randomPrisonNumber()

      stubNoMatchesPersonMatch()
      prisonDomainEventAndResponseSetup(
        PRISONER_CREATED,
        apiResponseSetup = ApiResponseSetup(prisonNumber = prisonNumber, nationality = null, religion = null),
      )

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(personEntity.nationalities.size).isEqualTo(0)
        assertThat(personEntity.religion).isEqualTo(null)
      }

      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber))
    }

    @Test
    fun `should check sentence start date is not populated when primary sentence is false`() {
      val prisonNumber = randomPrisonNumber()
      val sentenceStartDate = randomDate()

      stubNoMatchesPersonMatch()
      prisonDomainEventAndResponseSetup(
        PRISONER_CREATED,
        apiResponseSetup = ApiResponseSetup(prisonNumber = prisonNumber, sentenceStartDate = sentenceStartDate, primarySentence = false),
      )

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(personEntity.sentenceInfo.size).isEqualTo(0)
      }

      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber))
    }

    @Test
    fun `should receive the message successfully when prisoner updated event published`() {
      val prisonNumber = randomPrisonNumber()
      val prisoner = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))

      val updatedFirstName = randomName()
      val ethnicity = randomPrisonEthnicity()
      val updatedNationality = randomPrisonNationalityCode()
      val title = randomTitle()
      val updatedSexCode = randomPrisonSexCode()

      val updatedAliasGender = randomPrisonSexCode()
      val updatedAlias = ApiResponseSetupAlias(title = randomTitle(), firstName = randomName(), lastName = randomName(), gender = updatedAliasGender.key)

      stubNoMatchesPersonMatch(matchId = prisoner.matchId)
      prisonDomainEventAndResponseSetup(
        PRISONER_UPDATED,
        apiResponseSetup = ApiResponseSetup(gender = updatedSexCode.key, title = title, prisonNumber = prisonNumber, firstName = updatedFirstName, nationality = updatedNationality, ethnicity = ethnicity, aliases = listOf(updatedAlias)),
      )
      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      checkEventLogExist(prisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber = prisonNumber)!!
        assertThat(personEntity.matchId).isEqualTo(prisoner.matchId)
        val storedTitle = title.getTitle()
        assertThat(personEntity.getPrimaryName().titleCodeLegacy?.code).isEqualTo(storedTitle.code)
        assertThat(personEntity.getPrimaryName().titleCodeLegacy?.description).isEqualTo(storedTitle.description)
        assertThat(personEntity.getPrimaryName().titleCode).isEqualTo(TitleCode.from(title))
        assertThat(personEntity.getPrimaryName().firstName).isEqualTo(updatedFirstName)
        assertThat(personEntity.getPrimaryName().sexCode).isEqualTo(updatedSexCode.value)

        assertThat(personEntity.getAliases()).hasSize(1)
        assertThat(personEntity.getAliases()[0].sexCode).isEqualTo(updatedAliasGender.value)

        val storedPrisonEthnicity = ethnicity.getPrisonEthnicity()
        assertThat(personEntity.ethnicityCodeLegacy?.code).isEqualTo(storedPrisonEthnicity.code)
        assertThat(personEntity.ethnicityCodeLegacy?.description).isEqualTo(storedPrisonEthnicity.description)

        assertThat(personEntity.nationalities.size).isEqualTo(1)
        assertThat(personEntity.nationalities.first().nationalityCode?.name).isEqualTo(NationalityCode.fromPrisonMapping(updatedNationality)?.name)
        assertThat(personEntity.nationalities.first().nationalityCode?.description).isEqualTo(NationalityCode.fromPrisonMapping(updatedNationality)?.description)
      }
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
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      checkTelemetry(
        CPR_UUID_CREATED,
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
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
      awaitNotNull { personRepository.findByPrisonNumber(prisonNumber = prisonNumber) }
      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      expectNoMessagesOnQueueOrDlq(prisonEventsQueue)
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
      val pnc = randomShortPnc()
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
      prisonDomainEventAndResponseSetup(
        PRISONER_CREATED,
        apiResponseSetup = ApiResponseSetup(aliases = listOf(ApiResponseSetupAlias(firstName = aliasFirstName, middleName = aliasMiddleName, lastName = aliasLastName, dateOfBirth = aliasDateOfBirth)), firstName = firstName, middleName = middleName, lastName = lastName, prisonNumber = prisonNumber, pnc = pnc, sentenceStartDate = sentenceStartDate, primarySentence = true, cro = cro, addresses = listOf(ApiResponseSetupAddress(postcode = postcode, startDate = LocalDate.of(1970, 1, 1), noFixedAbode = true, fullAddress = "")), dateOfBirth = personDateOfBirth),
      )

      checkEventLog(prisonNumber, CPRLogEvents.CPR_RECORD_CREATED) { eventLogs ->
        assertThat(eventLogs.size).isEqualTo(1)
        val createdLog = eventLogs.first()
        assertThat(createdLog.pncs).isEqualTo(arrayOf(PNCIdentifier.from(pnc).pncId))
        assertThat(createdLog.cros).isEqualTo(arrayOf(cro))
        assertThat(createdLog.firstName).isEqualTo(firstName)
        assertThat(createdLog.middleNames).isEqualTo("$middleName $middleName")
        assertThat(createdLog.lastName).isEqualTo(lastName)
        assertThat(createdLog.dateOfBirth).isEqualTo(personDateOfBirth)
        assertThat(createdLog.sourceSystem).isEqualTo(NOMIS)
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

  private fun waitForMessageToBeProcessedAndDiscarded() {
    sleep(500)
  }
}
