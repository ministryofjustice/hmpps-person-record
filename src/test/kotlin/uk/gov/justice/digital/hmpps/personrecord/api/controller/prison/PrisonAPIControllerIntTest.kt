package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonCanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.BAHA
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.HUM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PrisonAPIControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return ok for get`() {
      val firstName = randomName()
      val lastName = randomName()
      val middleNames = randomName()
      val title = randomTitleCode()
      val disability = randomBoolean()
      val interestToImmigration = randomBoolean()
      val pnc = randomLongPnc()
      val noFixedAbode = true
      val startDate = randomDate()
      val endDate = randomDate()
      val postcode = randomPostcode()
      val nationality = randomNationalityCode()
      val religion = randomReligion()
      val ethnicity = randomPrisonEthnicity()
      val sex = randomPrisonSexCode()
      val sexualOrientation = randomPrisonSexualOrientation()

      val buildingName = randomName()
      val buildingNumber = randomBuildingNumber()
      val thoroughfareName = randomName()
      val dependentLocality = randomName()
      val postTown = randomName()

      val cro = randomCro()
      val crn = randomCrn()
      val prisonNumber = randomPrisonNumber()

      val person = createPersonWithNewKey(
        Person(
          firstName = randomName(),
          lastName = randomName(),
          middleNames = randomName(),
          dateOfBirth = randomDate(),
          sourceSystem = NOMIS,
          disability = disability,
          immigrationStatus = interestToImmigration,
          titleCode = title.value,
          crn = crn,
          sexCode = sex.value,
          sexualOrientation = sexualOrientation.value,
          prisonNumber = prisonNumber,
          nationalities = listOf(nationality),
          religion = religion,
          ethnicityCode = EthnicityCode.fromPrison(ethnicity),
          aliases = listOf(
            Alias(
              firstName = firstName,
              middleNames = middleNames,
              lastName = lastName,
              dateOfBirth = randomDate(),
              titleCode = title.value,
              sexCode = sex.value,
            ),
          ),
          addresses = listOf(
            Address(
              noFixedAbode = noFixedAbode,
              startDate = startDate,
              endDate = endDate,
              postcode = postcode,
              buildingName = buildingName,
              buildingNumber = buildingNumber,
              thoroughfareName = thoroughfareName,
              dependentLocality = dependentLocality,
              postTown = postTown,
            ),
          ),
          references = listOf(
            Reference(identifierType = IdentifierType.PNC, identifierValue = pnc),
            Reference(identifierType = IdentifierType.CRO, identifierValue = cro),
          ),
        ),
      )

      val existingPrisonReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, createRandomReligion()))

      val responseBody = webTestClient.get()
        .uri(prisonApiUrl(person.prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(PrisonCanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      val canonicalAlias = CanonicalAlias(
        firstName = firstName,
        lastName = lastName,
        middleNames = middleNames,
        title = CanonicalTitle.from(title.value),
        sex = CanonicalSex.from(sex.value),
      )
      val canonicalNationality = listOf(CanonicalNationality(nationality.name, nationality.description))
      val canonicalAddress = CanonicalAddress(
        noFixedAbode = noFixedAbode,
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        postcode = postcode,
        buildingName = buildingName,
        buildingNumber = buildingNumber,
        thoroughfareName = thoroughfareName,
        dependentLocality = dependentLocality,
        postTown = postTown,
      )
      val canonicalReligion = CanonicalReligion(code = religion, description = religion)
      val canonicalEthnicity = CanonicalEthnicity.from(EthnicityCode.fromPrison(ethnicity))
      assertThat(responseBody.record.cprUUID).isNull()
      assertThat(responseBody.record.firstName).isEqualTo(person.getPrimaryName().firstName)
      assertThat(responseBody.record.middleNames).isEqualTo(person.getPrimaryName().middleNames)
      assertThat(responseBody.record.lastName).isEqualTo(person.getPrimaryName().lastName)
      assertThat(responseBody.record.dateOfBirth).isEqualTo(person.getPrimaryName().dateOfBirth.toString())
      assertThat(responseBody.record.disability).isEqualTo(person.disability)
      assertThat(responseBody.record.interestToImmigration).isEqualTo(person.immigrationStatus)
      assertThat(responseBody.record.title.code).isEqualTo(person.getPrimaryName().titleCode?.name)
      assertThat(responseBody.record.title.description).isEqualTo(person.getPrimaryName().titleCode?.description)
      assertThat(responseBody.record.aliases.first().title.code).isEqualTo(person.getAliases().first().titleCode?.name)
      assertThat(responseBody.record.aliases.first().title.description).isEqualTo(
        person.getAliases().first().titleCode?.description,
      )
      assertThat(responseBody.record.nationalities.first().code).isEqualTo(canonicalNationality.first().code)
      assertThat(responseBody.record.nationalities.first().description).isEqualTo(canonicalNationality.first().description)
      assertThat(responseBody.record.aliases.first().sex.code).isEqualTo(sex.value.name)
      assertThat(responseBody.record.aliases.first().sex.description).isEqualTo(sex.value.description)

      assertThat(responseBody.record.sexualOrientation.code).isEqualTo(sexualOrientation.value.name)
      assertThat(responseBody.record.sexualOrientation.description).isEqualTo(sexualOrientation.value.description)
      assertThat(responseBody.record.religion.code).isEqualTo(canonicalReligion.code)
      assertThat(responseBody.record.religion.description).isEqualTo(canonicalReligion.description)
      assertThat(responseBody.record.ethnicity.code).isEqualTo(canonicalEthnicity.code)
      assertThat(responseBody.record.ethnicity.description).isEqualTo(canonicalEthnicity.description)
      assertThat(responseBody.record.aliases).isEqualTo(listOf(canonicalAlias))
      assertThat(responseBody.record.identifiers.cros).isEqualTo(listOf(cro))
      assertThat(responseBody.record.identifiers.pncs).isEqualTo(listOf(pnc))
      assertThat(responseBody.record.identifiers.crns).isEqualTo(listOf(crn))
      assertThat(responseBody.record.identifiers.prisonNumbers).isEqualTo(listOf(prisonNumber))
      assertThat(responseBody.record.addresses).isEqualTo(listOf(canonicalAddress))

      assertThat(responseBody.religionHistory.size).isEqualTo(1)
      assertThat(responseBody.religionHistory.first().startDate).isEqualTo(existingPrisonReligionEntity.startDate)
      assertThat(responseBody.religionHistory.first().endDate).isEqualTo(existingPrisonReligionEntity.endDate)
      assertThat(responseBody.religionHistory.first().religionCode).isEqualTo(existingPrisonReligionEntity.code)
      assertThat(responseBody.religionHistory.first().religionDescription).isEqualTo(ReligionCode.valueOf(existingPrisonReligionEntity.code!!).description)
      assertThat(responseBody.religionHistory.first().changeReasonKnown).isEqualTo(existingPrisonReligionEntity.changeReasonKnown)
      assertThat(responseBody.religionHistory.first().modifyDateTime).isEqualTo(existingPrisonReligionEntity.modifyDateTime)
      assertThat(responseBody.religionHistory.first().modifyUserId).isEqualTo(existingPrisonReligionEntity.modifyUserId)
      assertThat(responseBody.religionHistory.first().createDateTime).isEqualTo(existingPrisonReligionEntity.createDateTime)
      assertThat(responseBody.religionHistory.first().createUserId).isEqualTo(existingPrisonReligionEntity.createUserId)
      assertThat(responseBody.religionHistory.first().current).isEqualTo(existingPrisonReligionEntity.prisonRecordType.value)
      assertThat(responseBody.religionHistory.first().endDate).isEqualTo(existingPrisonReligionEntity.endDate)
    }

    @Test
    fun `should sort religions by start date and created date newest first`() {
      val prisonNumber = randomPrisonNumber()
      val person = createPerson(createRandomPrisonPersonDetails(prisonNumber = prisonNumber))
      createPersonKey()
        .addPerson(person)
      val now = LocalDate.now()
      val nowTime = LocalDateTime.now()

      sendPostRequestAsserted<Unit>(
        url = "/person/prison/$prisonNumber/religion",
        body = createRandomReligion().copy( // <- first in history to be written
          religionCode = BAHA.name,
          startDate = now.minusDays(1),
          endDate = now.minusDays(1),
          current = false,
          modifyDateTime = nowTime,
          createDateTime = nowTime.minusDays(1).minusHours(2),
        ),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.CREATED,
      )

      sendPostRequestAsserted<Unit>(
        url = "/person/prison/$prisonNumber/religion",
        body = createRandomReligion().copy( // <- second in history to be written
          religionCode = HUM.name,
          startDate = now.minusDays(1),
          endDate = now,
          current = false,
          modifyDateTime = nowTime,
          createDateTime = nowTime.minusDays(1).minusHours(1),
        ),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.CREATED,
      )

      sendPostRequestAsserted<Unit>(
        url = "/person/prison/$prisonNumber/religion",
        body = createRandomReligion().copy( // <- most recent in history to be written
          religionCode = AGNO.name,
          startDate = now,
          endDate = null,
          modifyDateTime = null,
          modifyUserId = null,
          current = true,
          createDateTime = nowTime,
        ),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.CREATED,
      )

      val responseBody = webTestClient.get()
        .uri(prisonApiUrl(prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(PrisonCanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      assertThat(responseBody.religionHistory.size).isEqualTo(3)
      assertThat(responseBody.religionHistory.first().current).isEqualTo(true)
      assertThat(responseBody.religionHistory.first().endDate).isNull()
      assertThat(responseBody.religionHistory.first().modifyDateTime).isNull()
      assertThat(responseBody.religionHistory.first().modifyUserId).isNull()

      assertThat(responseBody.religionHistory[1].current).isEqualTo(false)
      assertThat(responseBody.religionHistory[1].religionCode).isEqualTo(HUM.name)
      assertThat(responseBody.religionHistory[1].startDate).isEqualTo(now.minusDays(1))
      assertThat(responseBody.religionHistory[1].createDateTime.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(nowTime.minusDays(1).minusHours(1).truncatedTo(ChronoUnit.SECONDS))
      assertThat(responseBody.religionHistory[2].current).isEqualTo(false)
      assertThat(responseBody.religionHistory[2].religionCode).isEqualTo(BAHA.name)
      assertThat(responseBody.religionHistory[2].startDate).isEqualTo(now.minusDays(1))
      assertThat(responseBody.religionHistory[2].createDateTime.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(nowTime.minusDays(1).minusHours(2).truncatedTo(ChronoUnit.SECONDS))
    }

    @Test
    fun `should add list of additional identifiers to the prison record`() {
      val personOneCro = randomCro()
      val personTwoCro = randomCro()

      val personOneCrn = randomCrn()
      val personTwoCrn = randomCrn()

      val personOnePnc = randomLongPnc()
      val personTwoPnc = randomLongPnc()

      val personOneNationalInsuranceNumber = randomNationalInsuranceNumber()
      val personTwoNationalInsuranceNumber = randomNationalInsuranceNumber()

      val personOneArrestSummonNumber = randomArrestSummonNumber()
      val personTwoArrestSummonNumber = randomArrestSummonNumber()

      val personOneDriversLicenseNumber = randomDriverLicenseNumber()
      val personTwoDriversLicenseNumber = randomDriverLicenseNumber()

      val personOneDefendantId = randomDefendantId()
      val personTwoDefendantId = randomDefendantId()

      val personOne = createPerson(
        Person(
          firstName = randomName(),
          lastName = randomName(),
          middleNames = randomName(),
          dateOfBirth = randomDate(),
          sourceSystem = NOMIS,
          crn = personOneCrn,
          prisonNumber = randomPrisonNumber(),
          nationalities = listOf(randomNationalityCode()),
          religion = randomReligion(),
          cId = randomCId(),
          defendantId = personOneDefendantId,
          masterDefendantId = personOneDefendantId,
          references = listOf(
            Reference(identifierType = IdentifierType.CRO, identifierValue = personOneCro),
            Reference(identifierType = IdentifierType.PNC, identifierValue = personOnePnc),
            Reference(
              identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER,
              identifierValue = personOneNationalInsuranceNumber,
            ),
            Reference(
              identifierType = IdentifierType.ARREST_SUMMONS_NUMBER,
              identifierValue = personOneArrestSummonNumber,
            ),
            Reference(
              identifierType = IdentifierType.DRIVER_LICENSE_NUMBER,
              identifierValue = personOneDriversLicenseNumber,
            ),
          ),
        ),
      )

      val personTwo = createPerson(
        Person(
          firstName = randomName(),
          lastName = randomName(),
          middleNames = randomName(),
          dateOfBirth = randomDate(),
          sourceSystem = NOMIS,
          crn = personTwoCrn,
          prisonNumber = randomPrisonNumber(),
          nationalities = listOf(randomNationalityCode()),
          religion = randomReligion(),
          cId = randomCId(),
          defendantId = personTwoDefendantId,
          masterDefendantId = personTwoDefendantId,
          references = listOf(
            Reference(identifierType = IdentifierType.CRO, identifierValue = personTwoCro),
            Reference(identifierType = IdentifierType.PNC, identifierValue = personTwoPnc),
            Reference(
              identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER,
              identifierValue = personTwoNationalInsuranceNumber,
            ),
            Reference(
              identifierType = IdentifierType.ARREST_SUMMONS_NUMBER,
              identifierValue = personTwoArrestSummonNumber,
            ),
            Reference(
              identifierType = IdentifierType.DRIVER_LICENSE_NUMBER,
              identifierValue = personTwoDriversLicenseNumber,
            ),
          ),
        ),
      )

      createPersonKey().addPerson(personOne).addPerson(personTwo)

      val responseBody = webTestClient.get()
        .uri(prisonApiUrl(personOne.prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(PrisonCanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      assertThat(responseBody.record.identifiers.cros).containsExactly(personOneCro)
      assertThat(responseBody.record.identifiers.pncs).containsExactly(personOnePnc)
      assertThat(responseBody.record.identifiers.nationalInsuranceNumbers).containsExactly(personOneNationalInsuranceNumber)
      assertThat(responseBody.record.identifiers.arrestSummonsNumbers).containsExactly(personOneArrestSummonNumber)
      assertThat(responseBody.record.identifiers.driverLicenseNumbers).containsExactly(personOneDriversLicenseNumber)
      assertThat(responseBody.record.identifiers.crns).containsExactlyInAnyOrderElementsOf(
        listOf(
          personOne.crn,
          personTwo.crn,
        ),
      )
      assertThat(responseBody.record.identifiers.defendantIds).containsExactlyInAnyOrderElementsOf(
        listOf(
          personOne.defendantId,
          personTwo.defendantId,
        ),
      )
      assertThat(responseBody.record.identifiers.prisonNumbers).containsExactlyInAnyOrderElementsOf(
        listOf(
          personOne.prisonNumber,
          personTwo.prisonNumber,
        ),
      )
      assertThat(responseBody.record.identifiers.cids).containsExactlyInAnyOrderElementsOf(
        listOf(
          personOne.cId,
          personTwo.cId,
        ),
      )
    }

    @Test
    fun `should return redirect when the requested prison record has been merged`() {
      val sourcePrisonNumber = randomPrisonNumber()
      val targetPrisonNumber = randomPrisonNumber()

      val sourcePersonEntity = createPerson(createRandomPrisonPersonDetails(sourcePrisonNumber))
      val targetPersonEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(targetPrisonNumber))

      mergeRecord(sourcePersonEntity, targetPersonEntity)

      webTestClient.get()
        .uri(prisonApiUrl(sourcePrisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .is3xxRedirection
        .expectHeader()
        .valueEquals("Location", "/person/prison/$targetPrisonNumber")
    }
  }

  @Nested
  inner class ErrorScenarios {

    @Test
    fun `should return not found 404 with userMessage to show that the prisonNumber is not found`() {
      val prisonNumber = randomPrisonNumber()
      val expectedErrorMessage = "Not found: $prisonNumber"
      webTestClient.get()
        .uri(prisonApiUrl(prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.get()
        .uri(prisonApiUrl("accessdenied"))
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      webTestClient.get()
        .uri(prisonApiUrl("unauthorised"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun prisonApiUrl(prisonNumber: String?) = "/person/prison/$prisonNumber"
}
