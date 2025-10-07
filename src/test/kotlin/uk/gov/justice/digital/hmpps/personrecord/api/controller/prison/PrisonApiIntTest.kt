package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Nationality
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformEthnicity
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitle

class PrisonApiIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return ok for get`() {
      val firstName = randomName()
      val lastName = randomName()
      val middleNames = randomName()
      val title = randomTitle()
      val pnc = randomLongPnc()
      val noFixedAbode = true
      val startDate = randomDate()
      val endDate = randomDate()
      val postcode = randomPostcode()
      val nationality = randomNationalityCode()
      val religion = randomReligion()
      val ethnicity = randomCommonPlatformEthnicity()

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
          titleCode = TitleCode.from(title),
          crn = crn,
          sexCode = SexCode.M,
          prisonNumber = prisonNumber,
          nationalities = listOf(Nationality(nationality)),
          religion = religion,
          ethnicityCode = EthnicityCode.fromCommonPlatform(ethnicity),
          aliases = listOf(
            Alias(
              firstName = firstName,
              middleNames = middleNames,
              lastName = lastName,
              dateOfBirth = randomDate(),
              titleCode = TitleCode.from(title),
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

      val responseBody = webTestClient.get()
        .uri(prisonApiUrl(person.prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      val storedTitle = title.getTitle()
      val canonicalAlias = CanonicalAlias(
        firstName = firstName,
        lastName = lastName,
        middleNames = middleNames,
        title = CanonicalTitle(code = storedTitle.code, description = storedTitle.description),
      )
      val canonicalNationality = nationality.getEntity()?.let { listOf(CanonicalNationality(it.code, it.description)) }
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
      val canonicalEthnicity = CanonicalEthnicity.from(ethnicity.getCommonPlatformEthnicity())
      assertThat(responseBody.cprUUID).isNull()
      assertThat(responseBody.firstName).isEqualTo(person.getPrimaryName().firstName)
      assertThat(responseBody.middleNames).isEqualTo(person.getPrimaryName().middleNames)
      assertThat(responseBody.lastName).isEqualTo(person.getPrimaryName().lastName)
      assertThat(responseBody.dateOfBirth).isEqualTo(person.getPrimaryName().dateOfBirth.toString())
      assertThat(responseBody.title.code).isEqualTo(person.getPrimaryName().titleCode?.code)
      assertThat(responseBody.title.description).isEqualTo(person.getPrimaryName().titleCode?.description)
      assertThat(responseBody.aliases.first().title.code).isEqualTo(person.getAliases().first().titleCode?.code)
      assertThat(responseBody.aliases.first().title.description).isEqualTo(
        person.getAliases().first().titleCode?.description,
      )
      assertThat(responseBody.nationalities.first().code).isEqualTo(canonicalNationality?.first()?.code)
      assertThat(responseBody.nationalities.first().description).isEqualTo(canonicalNationality?.first()?.description)
      assertThat(responseBody.sex.code).isEqualTo("M")
      assertThat(responseBody.sex.description).isEqualTo("Male")
      assertThat(responseBody.religion.code).isEqualTo(canonicalReligion.code)
      assertThat(responseBody.religion.description).isEqualTo(canonicalReligion.description)
      assertThat(responseBody.ethnicity.code).isEqualTo(canonicalEthnicity.code)
      assertThat(responseBody.ethnicity.description).isEqualTo(canonicalEthnicity.description)
      assertThat(responseBody.aliases).isEqualTo(listOf(canonicalAlias))
      assertThat(responseBody.identifiers.cros).isEqualTo(listOf(cro))
      assertThat(responseBody.identifiers.pncs).isEqualTo(listOf(pnc))
      assertThat(responseBody.identifiers.crns).isEqualTo(listOf(crn))
      assertThat(responseBody.identifiers.prisonNumbers).isEqualTo(listOf(prisonNumber))
      assertThat(responseBody.addresses).isEqualTo(listOf(canonicalAddress))
    }

    @Test
    fun `should add list of additional identifiers to the canonical record`() {
      val personKey = createPersonKey()

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
          nationalities = listOf(Nationality(randomNationalityCode())),
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
        personKey,
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
          nationalities = listOf(Nationality(randomNationalityCode())),
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
        personKey,
      )

      val responseBody = webTestClient.get()
        .uri(prisonApiUrl(personOne.prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
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
