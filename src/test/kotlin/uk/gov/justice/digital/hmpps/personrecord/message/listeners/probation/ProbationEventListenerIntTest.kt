package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.getEmail
import uk.gov.justice.digital.hmpps.personrecord.extensions.getHome
import uk.gov.justice.digital.hmpps.personrecord.extensions.getMobile
import uk.gov.justice.digital.hmpps.personrecord.extensions.getPNCs
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NationalityEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_PERSONAL_DETAILS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitle
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupContact
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
      val pnc = randomLongPnc()
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
      val aliasGender = randomProbationSexCode()
      val gender = randomProbationSexCode()
      val sexualOrientation = randomProbationSexualOrientation()
      val religion = randomReligion()

      val homePhoneNumber = randomPhoneNumber()
      val mobilePhoneNumber = randomPhoneNumber()
      val email = randomEmail()

      val buildingName = randomName()
      val addressNumber = randomAddressNumber()
      val streetName = randomName()
      val district = randomName()
      val townCity = randomName()
      val county = randomName()
      val uprn = randomUprn()
      val notes = randomName()

      val dateOfBirth = randomDate()
      val dateOfDeath = randomDate()
      val apiResponse = ApiResponseSetup(
        dateOfBirth = dateOfBirth,
        dateOfDeath = dateOfDeath,
        crn = crn,
        pnc = pnc,
        title = title,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        prisonNumber = prisonNumber,
        cro = cro,
        addresses = listOf(
          ApiResponseSetupAddress(
            noFixedAbode = true,
            addressStartDate,
            addressEndDate,
            postcode = "LS1 1AB",
            fullAddress = "abc street",
            buildingName = buildingName,
            addressNumber = addressNumber,
            streetName = streetName,
            district = district,
            townCity = townCity,
            county = county,
            uprn = uprn,
            notes = notes,

          ),
          ApiResponseSetupAddress(postcode = "M21 9LX", fullAddress = "abc street"),
        ),
        aliases = listOf(
          ApiResponseSetupAlias(
            firstName = aliasFirstName,
            middleName = aliasMiddleName,
            lastName = aliasLastName,
            dateOfBirth = aliasDateOfBirth,
            gender = aliasGender.key,
          ),
        ),
        contacts = listOf(
          ApiResponseSetupContact(ContactType.HOME, homePhoneNumber),
          ApiResponseSetupContact(ContactType.MOBILE, mobilePhoneNumber),
          ApiResponseSetupContact(ContactType.EMAIL, email),
        ),
        ethnicity = ethnicity,
        nationality = nationality,
        secondaryNationality = secondaryNationality,
        sentences = listOf(ApiResponseSetupSentences(sentenceDate)),
        gender = gender.key,
        sexualOrientation = sexualOrientation.key,
        religion = religion,
      )
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, apiResponse)

      val personEntity = awaitNotNull { personRepository.findByCrn(crn) }

      assertThat(personEntity.personKey).isNotNull()
      assertThat(personEntity.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
      assertThat(personEntity.getPnc()).isEqualTo(pnc)
      assertThat(personEntity.crn).isEqualTo(crn)
      assertThat(personEntity.dateOfDeath).isEqualTo(dateOfDeath)
      val ethnicityCode = ethnicity.getProbationEthnicity()
      assertThat(personEntity.ethnicityCodeLegacy?.code).isEqualTo(ethnicityCode.code)
      assertThat(personEntity.ethnicityCodeLegacy?.description).isEqualTo(ethnicityCode.description)

      assertThat(personEntity.sentenceInfo[0].sentenceDate).isEqualTo(sentenceDate)
      assertThat(personEntity.getCro()).isEqualTo(cro)
      assertThat(personEntity.getAliases().size).isEqualTo(1)
      assertThat(personEntity.getAliases()[0].firstName).isEqualTo(aliasFirstName)
      assertThat(personEntity.getAliases()[0].middleNames).isEqualTo(aliasMiddleName)
      assertThat(personEntity.getAliases()[0].lastName).isEqualTo(aliasLastName)
      assertThat(personEntity.getAliases()[0].dateOfBirth).isEqualTo(aliasDateOfBirth)
      assertThat(personEntity.getAliases()[0].nameType).isEqualTo(NameType.ALIAS)
      assertThat(personEntity.getAliases()[0].sexCode).isEqualTo(aliasGender.value)
      assertThat(personEntity.getPrimaryName().firstName).isEqualTo(firstName)
      assertThat(personEntity.getPrimaryName().middleNames).isEqualTo(middleName)
      assertThat(personEntity.getPrimaryName().lastName).isEqualTo(lastName)
      assertThat(personEntity.getPrimaryName().nameType).isEqualTo(NameType.PRIMARY)
      assertThat(personEntity.getPrimaryName().sexCode).isEqualTo(gender.value)
      val storedTitle = title.getTitle()
      assertThat(personEntity.getPrimaryName().titleCodeLegacy?.code).isEqualTo(storedTitle.code)
      assertThat(personEntity.getPrimaryName().titleCodeLegacy?.description).isEqualTo(storedTitle.description)
      assertThat(personEntity.getPrimaryName().titleCode).isEqualTo(TitleCode.from(title))
      assertThat(personEntity.getPrimaryName().dateOfBirth).isEqualTo(dateOfBirth)

      assertThat(personEntity.addresses.size).isEqualTo(2)
      assertThat(personEntity.addresses[0].noFixedAbode).isEqualTo(true)
      assertThat(personEntity.addresses[0].startDate).isEqualTo(addressStartDate)
      assertThat(personEntity.addresses[0].endDate).isEqualTo(addressEndDate)
      assertThat(personEntity.addresses[0].postcode).isEqualTo("LS1 1AB")
      assertThat(personEntity.addresses[0].fullAddress).isEqualTo("abc street")
      assertThat(personEntity.addresses[0].type).isEqualTo(null)
      assertThat(personEntity.addresses[0].buildingName).isEqualTo(buildingName)
      assertThat(personEntity.addresses[0].buildingNumber).isEqualTo(addressNumber)
      assertThat(personEntity.addresses[0].thoroughfareName).isEqualTo(streetName)
      assertThat(personEntity.addresses[0].dependentLocality).isEqualTo(district)
      assertThat(personEntity.addresses[0].postTown).isEqualTo(townCity)
      assertThat(personEntity.addresses[0].county).isEqualTo(county)
      assertThat(personEntity.addresses[0].uprn).isEqualTo(uprn)
      assertThat(personEntity.addresses[0].comment).isEqualTo(notes)
      assertThat(personEntity.addresses[1].noFixedAbode).isNull()
      assertThat(personEntity.addresses[1].postcode).isEqualTo("M21 9LX")
      assertThat(personEntity.addresses[1].fullAddress).isEqualTo("abc street")
      assertThat(personEntity.addresses[1].type).isEqualTo(null)
      assertThat(personEntity.contacts.size).isEqualTo(3)
      assertThat(personEntity.contacts.getHome()?.contactValue).isEqualTo(homePhoneNumber)
      assertThat(personEntity.contacts.getMobile()?.contactValue).isEqualTo(mobilePhoneNumber)
      assertThat(personEntity.contacts.getEmail()?.contactValue).isEqualTo(email)
      assertThat(personEntity.matchId).isNotNull()
      assertThat(personEntity.lastModified).isNotNull()
      assertThat(personEntity.sexualOrientation).isEqualTo(sexualOrientation.value)
      assertThat(personEntity.religion).isEqualTo(religion)
      checkNationalities(personEntity.nationalities, nationality, secondaryNationality)

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
      checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_CREATED)
      checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    }

    @Test
    fun `should process personal details updated events successfully`() {
      val pnc = randomLongPnc()
      val crn = randomCrn()
      val gender = randomProbationSexCode()
      val originalEthnicity = randomProbationEthnicity()
      val nationality = randomProbationNationalityCode()
      val secondaryNationality = randomProbationNationalityCode()
      val originalTitle = "Mrs"
      probationDomainEventAndResponseSetup(
        NEW_OFFENDER_CREATED,
        ApiResponseSetup(
          crn = crn,
          pnc = pnc,
          gender = gender.key,
          ethnicity = originalEthnicity,
          title = originalTitle,
          nationality = nationality,
          secondaryNationality = secondaryNationality,
        ),
      )
      val personEntity = awaitNotNull { personRepository.findByCrn(crn) }
      assertThat(personEntity.getPnc()).isEqualTo(pnc)
      assertThat(personEntity.getPrimaryName().sexCode).isEqualTo(gender.value)
      assertThat(personEntity.dateOfDeath).isNull()
      val originalEthnicityCode = originalEthnicity.getProbationEthnicity()
      assertThat(personEntity.ethnicityCodeLegacy?.code).isEqualTo(originalEthnicityCode.code)
      assertThat(personEntity.ethnicityCodeLegacy?.description).isEqualTo(originalEthnicityCode.description)
      assertThat(personEntity.religion).isNull()
      assertThat(personEntity.getPrimaryName().titleCode).isEqualTo(TitleCode.from(originalTitle))

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val createdLastModified = personEntity.lastModified
      val changedPnc = randomLongPnc()
      val changedDateOfBirth = randomDate()
      val changedEthnicity = randomProbationEthnicity()
      val changedNationality = randomProbationNationalityCode()
      val changedSexCode = randomProbationSexCode()
      val sexualOrientation = randomProbationSexualOrientation()
      val aliasGender = randomProbationSexCode()
      val updatedReligion = randomReligion()
      val dateOfDeath = randomDate()
      val updatedTitle = "MR"
      probationDomainEventAndResponseSetup(
        OFFENDER_PERSONAL_DETAILS_UPDATED,
        ApiResponseSetup(
          crn = crn,
          pnc = changedPnc,
          gender = changedSexCode.key,
          dateOfBirth = changedDateOfBirth,
          ethnicity = changedEthnicity,
          nationality = changedNationality,
          title = updatedTitle,
          sexualOrientation = sexualOrientation.key,
          religion = updatedReligion,
          aliases = listOf(
            ApiResponseSetupAlias(lastName = randomName(), gender = aliasGender.key),
          ),
          dateOfDeath = dateOfDeath,
        ),
      )
      checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val updatedPersonEntity = awaitNotNull { personRepository.findByCrn(crn) }
      assertThat(updatedPersonEntity.getPnc()).isEqualTo(changedPnc)
      assertThat(updatedPersonEntity.dateOfDeath).isEqualTo(dateOfDeath)
      assertThat(updatedPersonEntity.getPrimaryName().sexCode).isEqualTo(changedSexCode.value)

      val updatedLastModified = updatedPersonEntity.lastModified

      assertThat(updatedLastModified).isAfter(createdLastModified)
      assertThat(updatedPersonEntity.getPrimaryName().dateOfBirth).isEqualTo(changedDateOfBirth)

      val changedEthnicityCode = changedEthnicity.getProbationEthnicity()
      assertThat(updatedPersonEntity.ethnicityCodeLegacy?.code).isEqualTo(changedEthnicityCode.code)
      assertThat(updatedPersonEntity.ethnicityCodeLegacy?.description).isEqualTo(changedEthnicityCode.description)

      checkNationalities(updatedPersonEntity.nationalities, changedNationality)

      val storedTitle = updatedTitle.getTitle()
      assertThat(updatedPersonEntity.getPrimaryName().titleCodeLegacy?.code).isEqualTo(storedTitle.code)
      assertThat(updatedPersonEntity.getPrimaryName().titleCodeLegacy?.description).isEqualTo(storedTitle.description)
      assertThat(updatedPersonEntity.getPrimaryName().titleCode).isEqualTo(TitleCode.from(updatedTitle))
      assertThat(updatedPersonEntity.sexualOrientation).isEqualTo(sexualOrientation.value)
      assertThat(updatedPersonEntity.religion).isEqualTo(updatedReligion)
      assertThat(updatedPersonEntity.getAliases()[0].sexCode).isEqualTo(aliasGender.value)
    }

    @Test
    fun `should link new probation record to an existing prison record`() {
      val crn = randomCrn()
      val prisonNumber = randomPrisonNumber()
      val firstName = randomName()
      val pnc = randomLongPnc()
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
      val existingPerson = createPerson(existingPrisoner)
      val personKeyEntity = createPersonKey().addPerson(existingPerson)

      stubOnePersonMatchAboveJoinThreshold(matchedRecord = existingPerson.matchId)

      val apiResponse = ApiResponseSetup(
        crn = crn,
        pnc = pnc,
        firstName = firstName,
        prisonNumber = prisonNumber,
        cro = cro,
        addresses = listOf(
          ApiResponseSetupAddress(postcode = postcode, fullAddress = "abc street"),
          ApiResponseSetupAddress(postcode = randomPostcode(), fullAddress = "abc street"),
        ),
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
    fun `should handle missing data correctly`() {
      val crn = randomCrn()
      val addresses = listOf(
        ApiResponseSetupAddress(postcode = null, noFixedAbode = null, startDate = null, endDate = null, fullAddress = null),
      )
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = null, addresses = addresses))
      val personEntity = awaitNotNull { personRepository.findByCrn(crn) }

      assertThat(personEntity.references.getPNCs()).isEmpty()
      assertThat(personEntity.addresses).hasSize(0)
    }

    @Test
    fun `should handle new offender details with an empty pnc`() {
      val crn = randomCrn()
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, pnc = ""))

      val personEntity = awaitNotNull { personRepository.findByCrn(crn) }

      assertThat(personEntity.references.getPNCs()).isEmpty()

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
      checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_CREATED)
    }

    @Test
    fun `should retry on 500 error`() {
      val crn = randomCrn()
      stub5xxResponse(probationUrl(crn), "next request will succeed", "retry")
      probationDomainEventAndResponseSetup(
        NEW_OFFENDER_CREATED,
        ApiResponseSetup(crn = crn, pnc = randomLongPnc()),
        scenario = "retry",
        currentScenarioState = "next request will succeed",
      )
      expectNoMessagesOnQueueOrDlq(probationEventsQueue)

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
      checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_CREATED)
    }

    @Test
    fun `multiple updates to single probation record are processed successfully`() {
      val pnc = randomLongPnc()
      val crn = randomCrn()
      blitz(30, 15) {
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
    fun `should deduplicate sentence dates`() {
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

      val personEntity = awaitNotNull { personRepository.findByCrn(crn) }

      assertThat(personEntity.sentenceInfo).hasSize(1)
    }

    @Test
    fun `should update + persist + delete reference entities when updating`() {
      val crn = randomCrn()
      val pnc = randomLongPnc()
      val cro = randomCro()
      val niNumber = randomNationalInsuranceNumber()

      probationDomainEventAndResponseSetup(
        NEW_OFFENDER_CREATED,
        ApiResponseSetup(crn = crn, pnc = pnc, cro = cro, nationalInsuranceNumber = niNumber),
      )

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val person = personRepository.findByCrn(crn)
      val pncEntity = person?.references?.find { it.identifierType == IdentifierType.PNC }
      val croEntity = person?.references?.find { it.identifierType == IdentifierType.CRO }

      val updatedCro = randomCro()
      probationDomainEventAndResponseSetup(
        OFFENDER_PERSONAL_DETAILS_UPDATED,
        ApiResponseSetup(crn = crn, pnc = pnc, cro = updatedCro, nationalInsuranceNumber = null),
      )

      checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val updatedPerson = awaitNotNull { personRepository.findByCrn(crn) }

      assertThat(updatedPerson.references).hasSize(2)

      val updatedPncEntity = updatedPerson.references.find { it.identifierType == IdentifierType.PNC }
      assertThat(updatedPncEntity?.identifierValue).isEqualTo(pnc)
      assertThat(updatedPncEntity?.id).isEqualTo(pncEntity?.id)
      assertThat(updatedPncEntity?.version).isEqualTo(pncEntity?.version)

      val updatedCroEntity = updatedPerson.references.find { it.identifierType == IdentifierType.CRO }
      assertThat(updatedCroEntity?.identifierValue).isEqualTo(updatedCro)
      assertThat(updatedCroEntity?.id).isNotEqualTo(croEntity?.id)
    }

    @Test
    fun `should update + persist + delete sentence entities when updating`() {
      val crn = randomCrn()

      val sentenceDateOne = randomDate()
      val sentenceDateTwo = randomDate()

      val sentenceDates = listOf(ApiResponseSetupSentences(sentenceDateOne), ApiResponseSetupSentences(sentenceDateTwo))
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, sentences = sentenceDates))

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val person = personRepository.findByCrn(crn)
      val sentenceDateOneEntity = person?.sentenceInfo?.find { it.sentenceDate == sentenceDateOne }
      val sentenceDateTwoEntity = person?.sentenceInfo?.find { it.sentenceDate == sentenceDateTwo }

      val sentenceDateFour = randomDate()
      val updateSentenceDates =
        listOf(ApiResponseSetupSentences(sentenceDateOne), ApiResponseSetupSentences(sentenceDateFour))
      probationDomainEventAndResponseSetup(
        OFFENDER_PERSONAL_DETAILS_UPDATED,
        ApiResponseSetup(crn = crn, sentences = updateSentenceDates),
      )

      checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val updatedPerson = awaitNotNull { personRepository.findByCrn(crn) }

      assertThat(updatedPerson.sentenceInfo).hasSize(2)

      val updatedSentenceDateOneEntity = updatedPerson.sentenceInfo.find { it.sentenceDate == sentenceDateOne }
      assertThat(updatedSentenceDateOneEntity?.id).isEqualTo(sentenceDateOneEntity?.id)
      assertThat(updatedSentenceDateOneEntity?.version).isEqualTo(sentenceDateOneEntity?.version)

      val updatedSentenceDateFourEntity = updatedPerson.sentenceInfo.find { it.sentenceDate == sentenceDateFour }
      assertThat(updatedSentenceDateFourEntity?.id).isNotEqualTo(sentenceDateTwoEntity?.id)
    }

    @Test
    fun `should update + persist + delete contact entities when updating`() {
      val crn = randomCrn()

      val homePhoneNumber = randomPhoneNumber()
      val mobilePhoneNumber = randomPhoneNumber()

      val contacts = listOf(ApiResponseSetupContact(ContactType.HOME, homePhoneNumber), ApiResponseSetupContact(ContactType.MOBILE, mobilePhoneNumber))
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, contacts = contacts))

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val person = personRepository.findByCrn(crn)
      val homePhoneNumberEntity = person?.contacts?.getHome()
      val mobilePhoneNumberEntity = person?.contacts?.getMobile()

      val email = randomEmail()
      val updateContacts = listOf(ApiResponseSetupContact(ContactType.HOME, homePhoneNumber), ApiResponseSetupContact(ContactType.EMAIL, email))
      probationDomainEventAndResponseSetup(OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = crn, contacts = updateContacts))

      checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val updatedPerson = awaitNotNull { personRepository.findByCrn(crn) }

      assertThat(updatedPerson.contacts).hasSize(2)

      val updatedHomePhoneNumberEntity = updatedPerson.contacts.getHome()
      assertThat(updatedHomePhoneNumberEntity?.id).isEqualTo(homePhoneNumberEntity?.id)
      assertThat(updatedHomePhoneNumberEntity?.version).isEqualTo(homePhoneNumberEntity?.version)

      val updatedEmailEntity = updatedPerson.contacts.getEmail()
      assertThat(updatedEmailEntity?.id).isNotEqualTo(mobilePhoneNumberEntity?.id)
    }

    @Test
    fun `should update + persist + delete address entities when updating`() {
      val crn = randomCrn()

      val postcodeOne = randomPostcode()
      val postcodeTwo = randomPostcode()

      val addresses = listOf(ApiResponseSetupAddress(postcode = postcodeOne, fullAddress = ""), ApiResponseSetupAddress(postcode = postcodeTwo))
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup(crn = crn, addresses = addresses))

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val person = personRepository.findByCrn(crn)

      val postcodeOneEntity = person?.addresses?.find { it.postcode == postcodeOne }
      val postcodeTwoEntity = person?.addresses?.find { it.postcode == postcodeTwo }

      val postcodeFour = randomPostcode()
      val updateAddresses = listOf(ApiResponseSetupAddress(postcode = postcodeOne), ApiResponseSetupAddress(postcode = postcodeFour))
      probationDomainEventAndResponseSetup(OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = crn, addresses = updateAddresses))

      checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

      val updatedPerson = awaitNotNull { personRepository.findByCrn(crn) }

      assertThat(updatedPerson.addresses).hasSize(2)

      val updatedPostcodeOneEntity = updatedPerson.addresses.find { it.postcode == postcodeOne }
      assertThat(updatedPostcodeOneEntity?.id).isEqualTo(postcodeOneEntity?.id)
      assertThat(updatedPostcodeOneEntity?.version).isEqualTo(postcodeOneEntity?.version)

      val updatedPostcodeFourEntity = updatedPerson.addresses.find { it.postcode == postcodeFour }
      assertThat(updatedPostcodeFourEntity?.id).isNotEqualTo(postcodeTwoEntity?.id)
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
  fun `should not push 404 from delius API to dead letter queue but discard message instead`() {
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
      val pnc = randomLongPnc()
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
    nationalityEntities: List<NationalityEntity>,
    vararg nationalities: String,
  ) {
    assertThat(nationalityEntities.size).isEqualTo(nationalities.size)
    val actual = nationalityEntities.map { it.nationalityCode }
    val expected = nationalities.map { NationalityCode.fromProbationMapping(it) }
    assertThat(actual).containsAll(expected)
  }
}
