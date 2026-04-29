package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.BAHA
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.HUM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonsNumber
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class DpsPrisonAPIControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `cluster size 1 - returns correct response for canonical record`() {
      val prisonNumber = randomPrisonNumber()
      val prisonPerson = createRandomPrisonPersonDetails(prisonNumber)
        .copy(
          contacts = listOf(Contact(MOBILE, randomPhoneNumber(), "+44")),
          nationalities = listOf(randomNationalityCode()),
        )
      val cluster = createPersonKey()
        .addPerson(prisonPerson)

      val person = cluster.personEntities.first()
      val existingPrisonReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, createRandomReligion()))

      val responseBody = sendGetRequestAsserted<DpsPrisonRecordTest>(
        url = prisonApiUrl(prisonNumber),
        roles = listOf(API_READ_ONLY),
        expectedStatus = OK,
      ).returnResult().responseBody!!

      val alias = prisonPerson.aliases.first()
      val canonicalAlias = CanonicalAlias(
        firstName = alias.firstName,
        lastName = alias.lastName,
        middleNames = alias.middleNames,
        title = CanonicalTitle.from(alias.titleCode),
        sex = CanonicalSex.from(alias.sexCode),
      )
      val nationality = prisonPerson.nationalities.first()
      val canonicalNationality = listOf(CanonicalNationality(nationality.name, nationality.description))
      val address = prisonPerson.addresses.first()
      val canonicalAddress = CanonicalAddress(
        noFixedAbode = address.noFixedAbode,
        startDate = address.startDate.toString(),
        endDate = address.endDate?.toString(),
        postcode = address.postcode,
        buildingName = address.buildingName,
        buildingNumber = address.buildingNumber,
        thoroughfareName = address.thoroughfareName,
        dependentLocality = address.dependentLocality,
        postTown = address.postTown,
      )
      val address2 = prisonPerson.addresses[1]
      val canonicalAddress2 = CanonicalAddress(
        noFixedAbode = address2.noFixedAbode,
        startDate = address2.startDate.toString(),
        endDate = address2.endDate?.toString(),
        postcode = address2.postcode,
        buildingName = address2.buildingName,
        buildingNumber = address2.buildingNumber,
        thoroughfareName = address2.thoroughfareName,
        dependentLocality = address2.dependentLocality,
        postTown = address2.postTown,
      )

      val canonicalReligion = CanonicalReligion(code = prisonPerson.religion, description = prisonPerson.religion)
      val canonicalEthnicity = CanonicalEthnicity.from(prisonPerson.ethnicityCode)
      assertThat(responseBody.cprUUID).isNull()
      assertThat(responseBody.firstName).isEqualTo(person.getPrimaryName().firstName)
      assertThat(responseBody.middleNames).isEqualTo(person.getPrimaryName().middleNames)
      assertThat(responseBody.lastName).isEqualTo(person.getPrimaryName().lastName)
      assertThat(responseBody.dateOfBirth).isEqualTo(person.getPrimaryName().dateOfBirth.toString())
      assertThat(responseBody.disability).isEqualTo(person.disability)
      assertThat(responseBody.interestToImmigration).isEqualTo(person.immigrationStatus)
      assertThat(responseBody.title.code).isEqualTo(person.getPrimaryName().titleCode?.name)
      assertThat(responseBody.title.description).isEqualTo(person.getPrimaryName().titleCode?.description)
      assertThat(responseBody.aliases.first().title.code).isEqualTo(person.getAliases().first().titleCode?.name)
      assertThat(responseBody.aliases.first().title.description).isEqualTo(
        person.getAliases().first().titleCode?.description,
      )
      assertThat(responseBody.nationalities.first().code).isEqualTo(canonicalNationality.first().code)
      assertThat(responseBody.nationalities.first().description).isEqualTo(canonicalNationality.first().description)
      assertThat(responseBody.aliases.first().sex.code).isEqualTo(canonicalAlias.sex.code)
      assertThat(responseBody.aliases.first().sex.description).isEqualTo(canonicalAlias.sex.description)

      assertThat(responseBody.sexualOrientation.code).isEqualTo(prisonPerson.sexualOrientation?.name)
      assertThat(responseBody.sexualOrientation.description).isEqualTo(prisonPerson.sexualOrientation?.description)
      assertThat(responseBody.religion.code).isEqualTo(canonicalReligion.code)
      assertThat(responseBody.religion.description).isEqualTo(canonicalReligion.description)
      assertThat(responseBody.ethnicity.code).isEqualTo(canonicalEthnicity.code)
      assertThat(responseBody.ethnicity.description).isEqualTo(canonicalEthnicity.description)
      assertThat(responseBody.aliases).isEqualTo(listOf(canonicalAlias))
      assertThat(responseBody.identifiers.cros).isEqualTo(listOf(prisonPerson.getCro()))
      assertThat(responseBody.identifiers.pncs).isEqualTo(listOf(prisonPerson.getPnc()))
      assertThat(responseBody.identifiers.prisonNumbers).isEqualTo(listOf(prisonNumber))
      assertThat(responseBody.addresses).usingRecursiveComparison().isEqualTo(listOf(canonicalAddress, canonicalAddress2))

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
        expectedStatus = CREATED,
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
        expectedStatus = CREATED,
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
        expectedStatus = CREATED,
      )

      val responseBody = webTestClient.get()
        .uri(prisonApiUrl(prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(DpsPrisonRecordTest::class.java)
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

      val personOneArrestSummonNumber = randomArrestSummonsNumber()
      val personTwoArrestSummonNumber = randomArrestSummonsNumber()

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
            Reference(identifierType = CRO, identifierValue = personOneCro),
            Reference(identifierType = PNC, identifierValue = personOnePnc),
            Reference(
              identifierType = NATIONAL_INSURANCE_NUMBER,
              identifierValue = personOneNationalInsuranceNumber,
            ),
            Reference(
              identifierType = ARREST_SUMMONS_NUMBER,
              identifierValue = personOneArrestSummonNumber,
            ),
            Reference(
              identifierType = DRIVER_LICENSE_NUMBER,
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
            Reference(identifierType = CRO, identifierValue = personTwoCro),
            Reference(identifierType = PNC, identifierValue = personTwoPnc),
            Reference(
              identifierType = NATIONAL_INSURANCE_NUMBER,
              identifierValue = personTwoNationalInsuranceNumber,
            ),
            Reference(
              identifierType = ARREST_SUMMONS_NUMBER,
              identifierValue = personTwoArrestSummonNumber,
            ),
            Reference(
              identifierType = DRIVER_LICENSE_NUMBER,
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
        .expectBody(DpsPrisonRecordTest::class.java)
        .returnResult()
        .responseBody!!

      assertThat(responseBody.identifiers.cros).containsExactly(personOneCro)
      assertThat(responseBody.identifiers.pncs).containsExactly(personOnePnc)
      assertThat(responseBody.identifiers.nationalInsuranceNumbers).containsExactly(personOneNationalInsuranceNumber)
      assertThat(responseBody.identifiers.arrestSummonsNumbers).containsExactly(personOneArrestSummonNumber)
      assertThat(responseBody.identifiers.driverLicenseNumbers).containsExactly(personOneDriversLicenseNumber)
      assertThat(responseBody.identifiers.crns).containsExactlyInAnyOrderElementsOf(
        listOf(
          personOne.crn,
          personTwo.crn,
        ),
      )
      assertThat(responseBody.identifiers.defendantIds).containsExactlyInAnyOrderElementsOf(
        listOf(
          personOne.defendantId,
          personTwo.defendantId,
        ),
      )
      assertThat(responseBody.identifiers.prisonNumbers).containsExactlyInAnyOrderElementsOf(
        listOf(
          personOne.prisonNumber,
          personTwo.prisonNumber,
        ),
      )
      assertThat(responseBody.identifiers.cids).containsExactlyInAnyOrderElementsOf(
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

      val targetPersonEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(targetPrisonNumber))
      createPerson(createRandomPrisonPersonDetails(sourcePrisonNumber)) { mergedTo = targetPersonEntity.id }

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

  private fun prisonApiUrl(prisonNumber: String?) = "/person/prison/dps/$prisonNumber"
}

// JsonUnwrapped annotation on DpsPrisonRecord produces this structure so we cannot use DpsPrisonRecord directly to mimic return value
// this is a copy/paste of CanonicalRecord with the additional properties of DpsPrisonRecord
data class DpsPrisonRecordTest(
  val cprUUID: String? = null,
  val firstName: String? = null,
  val middleNames: String? = null,
  val lastName: String? = null,
  val dateOfBirth: String? = null,
  val disability: Boolean? = null,
  val interestToImmigration: Boolean? = null,
  val title: CanonicalTitle,
  val sex: CanonicalSex,
  val sexualOrientation: CanonicalSexualOrientation,
  val religion: CanonicalReligion,
  val ethnicity: CanonicalEthnicity,
  val aliases: List<CanonicalAlias> = emptyList(),
  var nationalities: List<CanonicalNationality> = emptyList(),
  val addresses: List<CanonicalAddress> = emptyList(),
  val identifiers: CanonicalIdentifiers,
  val religionHistory: List<PrisonReligion>,
)
