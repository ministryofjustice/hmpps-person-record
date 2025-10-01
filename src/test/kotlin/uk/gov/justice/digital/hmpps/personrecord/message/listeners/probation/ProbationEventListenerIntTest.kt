package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.getPNCs
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_PERSONAL_DETAILS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitle
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupSentences
import java.util.UUID

class ProbationEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Nested
  inner class SuccessfulProcessing {
    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
    }

    @Test
    fun `creates person when new offender created event is published`() {
      val crn = randomCrn()
      val title = randomTitle()
      val prisonNumber = randomPrisonNumber()
      val firstName = randomName()
      val middleName = randomName() + " " + randomName()
      val lastName = randomName()
      val pnc = randomPnc()
      val cro = randomCro()
      val addressStartDate = randomDate()
      val addressEndDate = randomDate()
      val ethnicity = randomProbationEthnicity()
      val nationality = randomProbationNationalityCode()
      val secondaryNationality = randomProbationNationalityCode()
      val sentenceDate = randomDate()
      val aliasFirstName = randomName()
      val aliasMiddleName = randomName()
      val aliasLastName = randomName()
      val aliasDateOfBirth = randomDate()
      val gender = randomProbationSexCode()

      val dateOfBirth = randomDate()
      val apiResponse = ApiResponseSetup(
        dateOfBirth = dateOfBirth,
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
        aliases = listOf(ApiResponseSetupAlias(firstName = aliasFirstName, middleName = aliasMiddleName, lastName = aliasLastName, dateOfBirth = aliasDateOfBirth)),
        ethnicity = ethnicity,
        nationality = nationality,
        secondaryNationality = secondaryNationality,
        sentences = listOf(ApiResponseSetupSentences(sentenceDate)),
        gender = gender.key,
      )
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, apiResponse)

      val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }

      assertThat(personEntity.personKey).isNotNull()
      assertThat(personEntity.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
      assertThat(personEntity.getPnc()).isEqualTo(pnc)
      assertThat(personEntity.crn).isEqualTo(crn)
      val ethnicityCode = ethnicity.getProbationEthnicity()
      assertThat(personEntity.ethnicityCode?.code).isEqualTo(ethnicityCode.code)
      assertThat(personEntity.ethnicityCode?.description).isEqualTo(ethnicityCode.description)

      assertThat(personEntity.sentenceInfo[0].sentenceDate).isEqualTo(sentenceDate)
      assertThat(personEntity.getCro()).isEqualTo(cro)
      assertThat(personEntity.getAliases().size).isEqualTo(1)
      assertThat(personEntity.getAliases()[0].firstName).isEqualTo(aliasFirstName)
      assertThat(personEntity.getAliases()[0].middleNames).isEqualTo(aliasMiddleName)
      assertThat(personEntity.getAliases()[0].lastName).isEqualTo(aliasLastName)
      assertThat(personEntity.getAliases()[0].dateOfBirth).isEqualTo(aliasDateOfBirth)
      assertThat(personEntity.getAliases()[0].nameType).isEqualTo(NameType.ALIAS)
      assertThat(personEntity.getPrimaryName().firstName).isEqualTo(firstName)
      assertThat(personEntity.getPrimaryName().middleNames).isEqualTo(middleName)
      assertThat(personEntity.getPrimaryName().lastName).isEqualTo(lastName)
      assertThat(personEntity.getPrimaryName().nameType).isEqualTo(NameType.PRIMARY)
      val storedTitle = title.getTitle()
      assertThat(personEntity.getPrimaryName().titleCode?.code).isEqualTo(storedTitle.code)
      assertThat(personEntity.getPrimaryName().titleCode?.description).isEqualTo(storedTitle.description)
      assertThat(personEntity.getPrimaryName().dateOfBirth).isEqualTo(dateOfBirth)

      assertThat(personEntity.addresses.size).isEqualTo(2)
      assertThat(personEntity.addresses[0].noFixedAbode).isEqualTo(true)
      assertThat(personEntity.addresses[0].startDate).isEqualTo(addressStartDate)
      assertThat(personEntity.addresses[0].endDate).isEqualTo(addressEndDate)
      assertThat(personEntity.addresses[0].postcode).isEqualTo("LS1 1AB")
      assertThat(personEntity.addresses[0].fullAddress).isEqualTo("abc street")
      assertThat(personEntity.addresses[0].type).isEqualTo(null)
      assertThat(personEntity.addresses[1].noFixedAbode).isNull()
      assertThat(personEntity.addresses[1].postcode).isEqualTo("M21 9LX")
      assertThat(personEntity.addresses[1].fullAddress).isEqualTo("abc street")
      assertThat(personEntity.addresses[1].type).isEqualTo(null)
      assertThat(personEntity.contacts.size).isEqualTo(3)
      assertThat(personEntity.contacts[0].contactType).isEqualTo(ContactType.HOME)
      assertThat(personEntity.contacts[0].contactValue).isEqualTo("01234567890")
      assertThat(personEntity.contacts[1].contactType).isEqualTo(ContactType.MOBILE)
      assertThat(personEntity.contacts[1].contactValue).isEqualTo("01234567890")
      assertThat(personEntity.contacts[2].contactType).isEqualTo(ContactType.EMAIL)
      assertThat(personEntity.contacts[2].contactValue).isEqualTo("test@gmail.com")
      assertThat(personEntity.matchId).isNotNull()
      assertThat(personEntity.lastModified).isNotNull()
      assertThat(personEntity.sexCode).isEqualTo(gender.value)
      checkNationalities(personEntity, nationality, secondaryNationality)

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
      checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_CREATED)
      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    }

    @Test
    fun `should link new probation record to an existing prison record`() {
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
      val existingPerson = createPerson(existingPrisoner, personKeyEntity = personKeyEntity)

      stubOnePersonMatchAboveJoinThreshold(matchedRecord = existingPerson.matchId)

      val apiResponse = ApiResponseSetup(
        crn = crn,
        pnc = pnc,
        firstName = firstName,
        prisonNumber = prisonNumber,
        cro = cro,
        addresses = listOf(ApiResponseSetupAddress(postcode = postcode, fullAddress = "abc street"), ApiResponseSetupAddress(postcode = randomPostcode(), fullAddress = "abc street")),
      )
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, apiResponse)

      checkTelemetry(
        CPR_CANDIDATE_RECORD_SEARCH,
        mapOf(
          "SOURCE_SYSTEM" to DELIUS.name,
          "RECORD_COUNT" to "1",
          "UUID_COUNT" to "1",
          "CRN" to crn,
          "ABOVE_JOIN_THRESHOLD_COUNT" to "1",
          "ABOVE_FRACTURE_THRESHOLD_COUNT" to "0",
          "BELOW_FRACTURE_THRESHOLD_COUNT" to "0",
        ),
      )
      checkTelemetry(
        CPR_CANDIDATE_RECORD_FOUND_UUID,
        mapOf(
          "SOURCE_SYSTEM" to DELIUS.name,
          "CLUSTER_SIZE" to "1",
          "UUID" to personKeyEntity.personUUID.toString(),
        ),
      )
      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
      checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_CREATED)

      val personKey = personKeyRepository.findByPersonUUID(personKeyEntity.personUUID)
      assertThat(personKey?.personEntities?.size).isEqualTo(2)
    }

    @Test
    fun `should not link a new probation record to a cluster if its not above the join threshold`() {
      val crn = randomCrn()

      val existingPerson = createPersonWithNewKey(createRandomProbationPersonDetails(randomCrn()))

      stubOnePersonMatchAboveFractureThreshold(matchedRecord = existingPerson.matchId)

      probationDomainEventAndResponseSetup(OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = crn))

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
      checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_CREATED)
      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      awaitNotNullPerson { personRepository.findByCrn(crn) }
    }

    @Test
    fun `should write offender without PNC if PNC is missing`() {
      val crn = randomCrn()
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = null))
      val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }

      assertThat(personEntity.references.getPNCs()).isEmpty()

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
      checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_CREATED)
      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    }

    @Test
    fun `should handle new offender details with an empty pnc`() {
      val crn = randomCrn()
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = ""))

      val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }

      assertThat(personEntity.references.getPNCs()).isEmpty()

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
      checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_CREATED)
    }

    @Test
    fun `should retry on 500 error`() {
      val crn = randomCrn()
      stub5xxResponse(probationUrl(crn), "next request will succeed", "retry")
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = randomPnc()), scenario = "retry", currentScenarioState = "next request will succeed")
      expectNoMessagesOnQueueOrDlq(probationEventsQueue)

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
      checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_CREATED)
    }

    @Test
    fun `test multiple requests to probation single record process successfully`() {
      val pnc = randomPnc()
      val crn = randomCrn()
      blitz(30, 2) {
        probationDomainEventAndResponseSetup(OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = crn, pnc = pnc))
      }

      expectNoMessagesOnQueueOrDlq(probationEventsQueue)
      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn),
      )
      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf(
          "SOURCE_SYSTEM" to "DELIUS",
          "CRN" to crn,
        ),
        29,
      )
    }

    @Test
    fun `should process personals details updated events successfully`() {
      val pnc = randomPnc()
      val crn = randomCrn()
      val gender = randomProbationSexCode()
      val originalEthnicity = randomProbationEthnicity()
      val nationality = randomProbationNationalityCode()
      val secondaryNationality = randomProbationNationalityCode()
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = pnc, gender = gender.key, ethnicity = originalEthnicity, title = "Mrs", nationality = nationality, secondaryNationality = secondaryNationality))
      val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }
      assertThat(personEntity.getPnc()).isEqualTo(pnc)
      assertThat(personEntity.sexCode).isEqualTo(gender.value)
      val originalEthnicityCode = originalEthnicity.getProbationEthnicity()
      assertThat(personEntity.ethnicityCode?.code).isEqualTo(originalEthnicityCode.code)
      assertThat(personEntity.ethnicityCode?.description).isEqualTo(originalEthnicityCode.description)

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val createdLastModified = personEntity.lastModified
      val changedPnc = randomPnc()
      val changedDateOfBirth = randomDate()
      val changedEthnicity = randomProbationEthnicity()
      val changedNationality = randomProbationNationalityCode()
      val changedSexCode = randomProbationSexCode()
      probationDomainEventAndResponseSetup(OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = crn, pnc = changedPnc, gender = changedSexCode.key, dateOfBirth = changedDateOfBirth, ethnicity = changedEthnicity, nationality = changedNationality, title = "MR"))
      checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val updatedPersonEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }
      assertThat(updatedPersonEntity.getPnc()).isEqualTo(changedPnc)
      assertThat(updatedPersonEntity.sexCode).isEqualTo(changedSexCode.value)

      val updatedLastModified = updatedPersonEntity.lastModified

      assertThat(updatedLastModified).isAfter(createdLastModified)
      assertThat(updatedPersonEntity.getPrimaryName().dateOfBirth).isEqualTo(changedDateOfBirth)

      val changedEthnicityCode = changedEthnicity.getProbationEthnicity()
      assertThat(updatedPersonEntity.ethnicityCode?.code).isEqualTo(changedEthnicityCode.code)
      assertThat(updatedPersonEntity.ethnicityCode?.description).isEqualTo(changedEthnicityCode.description)

      checkNationalities(updatedPersonEntity, changedNationality)

      assertThat(updatedPersonEntity.getPrimaryName().titleCode?.code).isEqualTo("MR")
      assertThat(updatedPersonEntity.getPrimaryName().titleCode?.description).isEqualTo("Mr")
    }

    @Test
    fun `should deduplicate sentences dates`() {
      val crn = randomCrn()
      val sentenceDate = randomDate()

      val apiResponse = ApiResponseSetup(
        crn = crn,
        sentences = listOf(
          ApiResponseSetupSentences(sentenceDate),
          ApiResponseSetupSentences(sentenceDate),
          ApiResponseSetupSentences(sentenceDate),
        ),
      )

      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, apiResponse)

      val personEntity = awaitNotNullPerson { personRepository.findByCrn(crn) }

      assertThat(personEntity.sentenceInfo).hasSize(1)
    }
  }

  @Test
  fun `person match returns a match ID which does not exist in hmpps-person-record and a new UUID is created`() {
    val crn = randomCrn()
    stubPersonMatchUpsert()
    val matchIdWhichExistsInPersonMatchButNotInCPR = UUID.randomUUID().toString()
    val highConfidenceMatchWhichDoesNotExistInCPR = PersonMatchScore(matchIdWhichExistsInPersonMatchButNotInCPR, 0.99999F, 24F, candidateShouldJoin = true, candidateShouldFracture = false)
    stubPersonMatchScores(personMatchResponse = listOf(highConfidenceMatchWhichDoesNotExistInCPR))
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn))
    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should not push 404 to dead letter queue but discard message instead`() {
    val crn = randomCrn()
    stub404Response(probationUrl(crn))
    val domainEvent = probationDomainEvent(NEW_OFFENDER_CREATED, crn)
    publishDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    expectNoMessagesOnQueueOrDlq(probationEventsQueue)
  }

  @Test
  fun `should put message on dlq when message processing fails`() {
    val crn = randomCrn()
    stub5xxResponse(probationUrl(crn), nextScenarioState = "request will fail", "failure")
    stub5xxResponse(probationUrl(crn), currentScenarioState = "request will fail", nextScenarioState = "request will fail", scenarioName = "failure")
    stub5xxResponse(probationUrl(crn), currentScenarioState = "request will fail", nextScenarioState = "request will fail", scenarioName = "failure")
    val domainEvent = probationDomainEvent(NEW_OFFENDER_CREATED, crn)
    publishDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    expectOneMessageOnDlq(probationEventsQueue)
  }

  @Nested
  inner class EventLog {

    @Test
    fun `should save record details to event log on create`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val crn = randomCrn()
      val firstName = randomName()
      val middleName = randomName()
      val lastName = randomName()
      val pnc = randomPnc()
      val cro = randomCro()
      val postcode = randomPostcode()
      val sentenceDate = randomDate()
      val aliasFirstName = randomName()
      val aliasLastName = randomName()
      val aliasDateOfBirth = randomDate()

      val apiResponse = ApiResponseSetup(
        crn = crn,
        pnc = pnc,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        cro = cro,
        addresses = listOf(
          ApiResponseSetupAddress(postcode = postcode, fullAddress = ""),
        ),
        aliases = listOf(ApiResponseSetupAlias(firstName = aliasFirstName, middleName = "", lastName = aliasLastName, dateOfBirth = aliasDateOfBirth)),
        sentences = listOf(ApiResponseSetupSentences(sentenceDate)),
      )
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, apiResponse)

      checkEventLog(crn, CPRLogEvents.CPR_RECORD_CREATED) { eventLogs ->
        assertThat(eventLogs.size).isEqualTo(1)
        val createdLog = eventLogs.first()
        assertThat(createdLog.pncs).isEqualTo(arrayOf(pnc))
        assertThat(createdLog.cros).isEqualTo(arrayOf(cro))
        assertThat(createdLog.firstName).isEqualTo(firstName)
        assertThat(createdLog.middleNames).isEqualTo(middleName)
        assertThat(createdLog.lastName).isEqualTo(lastName)
        assertThat(createdLog.sourceSystem).isEqualTo(DELIUS)
        assertThat(createdLog.postcodes).isEqualTo(arrayOf(postcode))
        assertThat(createdLog.sentenceDates).isEqualTo(arrayOf(sentenceDate))
        assertThat(createdLog.firstNameAliases).isEqualTo(arrayOf(aliasFirstName))
        assertThat(createdLog.lastNameAliases).isEqualTo(arrayOf(aliasLastName))
        assertThat(createdLog.dateOfBirthAliases).isEqualTo(arrayOf(aliasDateOfBirth))
        assertThat(createdLog.personUUID).isNotNull()
        assertThat(createdLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
      }
      checkEventLogExist(crn, CPRLogEvents.CPR_UUID_CREATED)
    }
  }

  private fun checkNationalities(
    person: PersonEntity,
    vararg nationalities: String,
  ) {
    assertThat(person.nationalities.size).isEqualTo(nationalities.size)
    val actual = person.nationalities.map { Pair(it.nationalityCode?.code, it.nationalityCode?.description) }
    val expected = nationalities.map { it.getNationalityCodeEntityFromProbationCode() }.map { Pair(it?.code, it?.description) }
    assertThat(actual).containsAll(expected)
  }
}
