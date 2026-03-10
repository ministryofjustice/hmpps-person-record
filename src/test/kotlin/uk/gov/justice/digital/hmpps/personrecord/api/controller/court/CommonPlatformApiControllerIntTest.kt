package uk.gov.justice.digital.hmpps.personrecord.api.controller.court

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformSexCode
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode

class CommonPlatformApiControllerIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return when all fields populated`() {
      val firstName = randomName()
      val lastName = randomName()
      val middleNames = randomName()
      val title = randomTitleCode()
      val pnc = randomLongPnc()
      val noFixedAbode = true
      val startDate = randomDate()
      val endDate = randomDate()
      val postcode = randomPostcode()
      val nationality = randomNationalityCode()
      val religion = randomReligion()
      val ethnicity = randomCommonPlatformEthnicity()
      val sex = randomCommonPlatformSexCode()
      val sexualOrientation = randomPrisonSexualOrientation().value

      val buildingName = randomName()
      val buildingNumber = randomBuildingNumber()
      val thoroughfareName = randomName()
      val dependentLocality = randomName()
      val postTown = randomName()

      val cro = randomCro()
      val crn = randomCrn()
      val defendantId = randomDefendantId()
      val prisonNumber = randomPrisonNumber()
      val cid = randomCId()

      val person = createPersonWithNewKey(
        Person(
          firstName = randomName(),
          lastName = randomName(),
          middleNames = randomName(),
          dateOfBirth = randomDate(),
          disability = randomBoolean(),
          immigrationStatus = randomBoolean(),
          sourceSystem = COMMON_PLATFORM,
          titleCode = title.value,
          crn = crn,
          sexCode = sex.value,
          sexualOrientation = sexualOrientation,
          prisonNumber = prisonNumber,
          nationalities = listOf(nationality),
          religion = religion,
          cId = cid,
          ethnicityCode = EthnicityCode.fromCommonPlatform(ethnicity),
          defendantId = defendantId,
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
            Reference(identifierType = PNC, identifierValue = pnc),
            Reference(identifierType = CRO, identifierValue = cro),
          ),
        ),
      )

      val responseBody = webTestClient.get()
        .uri(commonPlatformApiUrl(person.defendantId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
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
      val canonicalEthnicity = CanonicalEthnicity.from(EthnicityCode.fromCommonPlatform(ethnicity))
      Assertions.assertThat(responseBody.cprUUID).isNull()
      Assertions.assertThat(responseBody.firstName).isEqualTo(person.getPrimaryName().firstName)
      Assertions.assertThat(responseBody.middleNames).isEqualTo(person.getPrimaryName().middleNames)
      Assertions.assertThat(responseBody.lastName).isEqualTo(person.getPrimaryName().lastName)
      Assertions.assertThat(responseBody.dateOfBirth).isEqualTo(person.getPrimaryName().dateOfBirth.toString())
      Assertions.assertThat(responseBody.disability).isEqualTo(person.disability)
      Assertions.assertThat(responseBody.interestToImmigration).isEqualTo(person.immigrationStatus)
      Assertions.assertThat(responseBody.title.code).isEqualTo(person.getPrimaryName().titleCode?.name)
      Assertions.assertThat(responseBody.title.description).isEqualTo(person.getPrimaryName().titleCode?.description)
      Assertions.assertThat(responseBody.aliases.first().title.code).isEqualTo(person.getAliases().first().titleCode?.name)
      Assertions.assertThat(responseBody.aliases.first().title.description).isEqualTo(
        person.getAliases().first().titleCode?.description,
      )
      Assertions.assertThat(responseBody.aliases.first().sex.code).isEqualTo(person.getAliases().first().sexCode?.name)
      Assertions.assertThat(responseBody.aliases.first().sex.description).isEqualTo(person.getAliases().first().sexCode?.description)
      Assertions.assertThat(responseBody.nationalities.first().code).isEqualTo(canonicalNationality.first().code)
      Assertions.assertThat(responseBody.nationalities.first().description).isEqualTo(canonicalNationality.first().description)
      Assertions.assertThat(responseBody.sex.code).isEqualTo(sex.value.name)
      Assertions.assertThat(responseBody.sex.description).isEqualTo(sex.value.description)
      Assertions.assertThat(responseBody.sexualOrientation.code).isEqualTo(sexualOrientation.name)
      Assertions.assertThat(responseBody.sexualOrientation.description).isEqualTo(sexualOrientation.description)
      Assertions.assertThat(responseBody.religion.code).isEqualTo(canonicalReligion.code)
      Assertions.assertThat(responseBody.religion.description).isEqualTo(canonicalReligion.description)
      Assertions.assertThat(responseBody.ethnicity.code).isEqualTo(canonicalEthnicity.code)
      Assertions.assertThat(responseBody.ethnicity.description).isEqualTo(canonicalEthnicity.description)
      Assertions.assertThat(responseBody.aliases).isEqualTo(listOf(canonicalAlias))
      Assertions.assertThat(responseBody.identifiers.cros).isEqualTo(listOf(cro))
      Assertions.assertThat(responseBody.identifiers.pncs).isEqualTo(listOf(pnc))
      Assertions.assertThat(responseBody.identifiers.crns).isEqualTo(listOf(crn))
      Assertions.assertThat(responseBody.identifiers.defendantIds).isEqualTo(listOf(defendantId))
      Assertions.assertThat(responseBody.identifiers.prisonNumbers).isEqualTo(listOf(prisonNumber))
      Assertions.assertThat(responseBody.identifiers.cids).isEqualTo(listOf(cid))
      Assertions.assertThat(responseBody.addresses).isEqualTo(listOf(canonicalAddress))
    }

    @Test
    fun `should return nulls when values are null or empty`() {
      val defendantId = randomDefendantId()

      val person = createPersonWithNewKey(
        Person(
          sourceSystem = COMMON_PLATFORM,
          defendantId = defendantId,
        ),

      )

      val responseBody = webTestClient.get()
        .uri(commonPlatformApiUrl(person.defendantId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      Assertions.assertThat(responseBody.firstName).isNull()
      Assertions.assertThat(responseBody.middleNames).isNull()
      Assertions.assertThat(responseBody.lastName).isNull()
      Assertions.assertThat(responseBody.dateOfBirth).isNull()
      Assertions.assertThat(responseBody.title.code).isNull()
      Assertions.assertThat(responseBody.title.description).isNull()
      Assertions.assertThat(responseBody.ethnicity.code).isNull()
      Assertions.assertThat(responseBody.ethnicity.description).isNull()
      Assertions.assertThat(responseBody.sex.code).isNull()
      Assertions.assertThat(responseBody.sex.description).isNull()
      Assertions.assertThat(responseBody.religion.code).isNull()
      Assertions.assertThat(responseBody.religion.description).isNull()
      Assertions.assertThat(responseBody.title.code).isNull()
      Assertions.assertThat(responseBody.title.description).isNull()
      Assertions.assertThat(responseBody.ethnicity.code).isNull()
      Assertions.assertThat(responseBody.ethnicity.description).isNull()
      Assertions.assertThat(responseBody.sex.code).isNull()
      Assertions.assertThat(responseBody.sex.description).isNull()
      Assertions.assertThat(responseBody.religion.code).isNull()
      Assertions.assertThat(responseBody.religion.description).isNull()
      Assertions.assertThat(responseBody.nationalities).isEmpty()
      Assertions.assertThat(responseBody.aliases).isEmpty()
      Assertions.assertThat(responseBody.addresses).isEmpty()
      Assertions.assertThat(responseBody.identifiers.crns).isEmpty()
      Assertions.assertThat(responseBody.identifiers.defendantIds).isEqualTo(listOf(defendantId))
      Assertions.assertThat(responseBody.identifiers.prisonNumbers).isEmpty()
      Assertions.assertThat(responseBody.identifiers.cids).isEmpty()
      Assertions.assertThat(responseBody.identifiers.pncs).isEmpty()
      Assertions.assertThat(responseBody.identifiers.cros).isEmpty()
      Assertions.assertThat(responseBody.identifiers.nationalInsuranceNumbers).isEmpty()
      Assertions.assertThat(responseBody.identifiers.driverLicenseNumbers).isEmpty()
      Assertions.assertThat(responseBody.identifiers.arrestSummonsNumbers).isEmpty()
    }

    @Test
    fun `should return nulls when some alias and address values are null or empty`() {
      val defendantId = randomDefendantId()

      val aliasFirstName = randomName()
      val postcode = randomPostcode()
      val person = createPersonWithNewKey(
        Person(
          sourceSystem = COMMON_PLATFORM,
          defendantId = defendantId,
          aliases = listOf(Alias(firstName = aliasFirstName)),
          addresses = listOf(Address(postcode = postcode)),
        ),

      )

      val responseBody = webTestClient.get()
        .uri(commonPlatformApiUrl(person.defendantId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      Assertions.assertThat(responseBody.aliases.first().firstName).isEqualTo(aliasFirstName)
      Assertions.assertThat(responseBody.aliases.first().lastName).isNull()
      Assertions.assertThat(responseBody.aliases.first().middleNames).isNull()
      Assertions.assertThat(responseBody.aliases.first().title.code).isNull()
      Assertions.assertThat(responseBody.aliases.first().title.description).isNull()
      Assertions.assertThat(responseBody.addresses.first().postcode).isEqualTo(postcode)
      Assertions.assertThat(responseBody.addresses.first().startDate).isNull()
      Assertions.assertThat(responseBody.addresses.first().endDate).isNull()
      Assertions.assertThat(responseBody.addresses.first().noFixedAbode).isNull()
      Assertions.assertThat(responseBody.addresses.first().buildingName).isNull()
      Assertions.assertThat(responseBody.addresses.first().buildingNumber).isNull()
      Assertions.assertThat(responseBody.addresses.first().thoroughfareName).isNull()
      Assertions.assertThat(responseBody.addresses.first().dependentLocality).isNull()
      Assertions.assertThat(responseBody.addresses.first().postTown).isNull()
      Assertions.assertThat(responseBody.addresses.first().county).isNull()
      Assertions.assertThat(responseBody.addresses.first().country).isNull()
      Assertions.assertThat(responseBody.addresses.first().uprn).isNull()
    }

    @Test
    fun `should add list of additional identifiers off two people`() {
      val personOneCro = randomCro()
      val personTwoCro = randomCro()

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

      val personOne = createPerson(
        Person(
          firstName = randomName(),
          lastName = randomName(),
          middleNames = randomName(),
          dateOfBirth = randomDate(),
          sourceSystem = COMMON_PLATFORM,
          nationalities = listOf(randomNationalityCode()),
          religion = randomReligion(),
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
          sourceSystem = DELIUS,
          crn = personTwoCrn,
          nationalities = listOf(randomNationalityCode()),
          religion = randomReligion(),
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
        .uri(commonPlatformApiUrl(personOne.defendantId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      Assertions.assertThat(responseBody.identifiers.cros).containsExactly(personOneCro)
      Assertions.assertThat(responseBody.identifiers.pncs).containsExactly(personOnePnc)
      Assertions.assertThat(responseBody.identifiers.nationalInsuranceNumbers).containsExactly(personOneNationalInsuranceNumber)
      Assertions.assertThat(responseBody.identifiers.arrestSummonsNumbers).containsExactly(personOneArrestSummonNumber)
      Assertions.assertThat(responseBody.identifiers.driverLicenseNumbers).containsExactly(personOneDriversLicenseNumber)
      Assertions.assertThat(responseBody.identifiers.crns).containsExactlyInAnyOrderElementsOf(
        listOf(
          personTwo.crn,
        ),
      )
      Assertions.assertThat(responseBody.identifiers.defendantIds).containsExactlyInAnyOrderElementsOf(
        listOf(
          personOne.defendantId,
        ),
      )
    }

    @Test
    fun `should return linked crn`() {
      val crn = randomCrn()
      val defendantId = randomDefendantId()

      val defendant = createPersonWithNewKey(createRandomCommonPlatformPersonDetails(defendantId))

      val probationCase = ProbationCase(
        name = ProbationCaseName(firstName = randomName(), lastName = randomName()),
        identifiers = Identifiers(crn = crn),
      )

      stubPersonMatchUpsert()
      stubOnePersonMatchAboveJoinThreshold(matchedRecord = defendant.matchId)

      webTestClient.put()
        .uri("/person/probation/$defendantId")
        .authorised(listOf(PROBATION_API_READ_WRITE))
        .bodyValue(probationCase)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn),
      )

      val responseBody = webTestClient.get()
        .uri(commonPlatformApiUrl(defendantId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      Assertions.assertThat(responseBody.identifiers.crns.size).isEqualTo(1)
      Assertions.assertThat(responseBody.identifiers.crns.first()).isEqualTo(crn)
    }
  }

  @Nested
  inner class ErrorScenarios {

    @Test
    fun `should return not found 404 with userMessage to show that the defendantId is not found`() {
      val nonExistentDefendantId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
      val expectedErrorMessage = "Not found: $nonExistentDefendantId"
      webTestClient.get()
        .uri(commonPlatformApiUrl(nonExistentDefendantId))
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
        .uri(commonPlatformApiUrl("accessdenied"))
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
        .uri(commonPlatformApiUrl("unauthorised"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun commonPlatformApiUrl(defendantId: String?) = "/person/commonplatform/$defendantId"
}
