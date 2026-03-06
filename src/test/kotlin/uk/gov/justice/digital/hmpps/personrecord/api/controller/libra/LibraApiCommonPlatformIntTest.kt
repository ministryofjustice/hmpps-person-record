package uk.gov.justice.digital.hmpps.personrecord.api.controller.libra

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonNumber
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode

class LibraApiCommonPlatformIntTest : WebTestBase() {

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
          sourceSystem = SourceSystemType.LIBRA,
          titleCode = title.value,
          crn = crn,
          sexCode = sex.value,
          prisonNumber = prisonNumber,
          nationalities = listOf(nationality),
          religion = religion,
          cId = cid,
          ethnicityCode = EthnicityCode.Companion.fromCommonPlatform(ethnicity),
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
            Reference(identifierType = IdentifierType.PNC, identifierValue = pnc),
            Reference(identifierType = IdentifierType.CRO, identifierValue = cro),
          ),
        ),
      )

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(person.cId))
        .authorised(listOf(Roles.API_READ_ONLY))
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
        title = CanonicalTitle.Companion.from(title.value),
        sex = CanonicalSex.Companion.from(sex.value),
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
      val canonicalEthnicity = CanonicalEthnicity.Companion.from(EthnicityCode.Companion.fromCommonPlatform(ethnicity))
      Assertions.assertThat(responseBody.cprUUID).isNull()
      Assertions.assertThat(responseBody.firstName).isEqualTo(person.getPrimaryName().firstName)
      Assertions.assertThat(responseBody.middleNames).isEqualTo(person.getPrimaryName().middleNames)
      Assertions.assertThat(responseBody.lastName).isEqualTo(person.getPrimaryName().lastName)
      Assertions.assertThat(responseBody.dateOfBirth).isEqualTo(person.getPrimaryName().dateOfBirth.toString())
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
      val cId = randomCId()

      val person = createPersonWithNewKey(
        Person(
          sourceSystem = SourceSystemType.LIBRA,
          cId = cId,
        ),

      )

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(person.cId))
        .authorised(listOf(Roles.API_READ_ONLY))
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
      Assertions.assertThat(responseBody.identifiers.defendantIds).isEmpty()
      Assertions.assertThat(responseBody.identifiers.cids).isEqualTo(listOf(cId))
      Assertions.assertThat(responseBody.identifiers.prisonNumbers).isEmpty()
      Assertions.assertThat(responseBody.identifiers.pncs).isEmpty()
      Assertions.assertThat(responseBody.identifiers.cros).isEmpty()
      Assertions.assertThat(responseBody.identifiers.nationalInsuranceNumbers).isEmpty()
      Assertions.assertThat(responseBody.identifiers.driverLicenseNumbers).isEmpty()
      Assertions.assertThat(responseBody.identifiers.arrestSummonsNumbers).isEmpty()
    }

    @Test
    fun `should return nulls when some data (eg alias and address) values are null or empty`() {
      val cId = randomCId()

      val aliasFirstName = randomName()
      val postcode = randomPostcode()
      val person = createPersonWithNewKey(
        Person(
          sourceSystem = SourceSystemType.LIBRA,
          cId = cId,
          aliases = listOf(Alias(firstName = aliasFirstName)),
          addresses = listOf(Address(postcode = postcode)),
        ),

      )

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(person.cId))
        .authorised(listOf(Roles.API_READ_ONLY))
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

      val personOneCId = randomCId()

      val personOne = createPerson(
        Person(
          firstName = randomName(),
          lastName = randomName(),
          middleNames = randomName(),
          dateOfBirth = randomDate(),
          sourceSystem = SourceSystemType.LIBRA,
          nationalities = listOf(randomNationalityCode()),
          religion = randomReligion(),

          cId = personOneCId,
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
          sourceSystem = SourceSystemType.DELIUS,
          crn = personTwoCrn,
          nationalities = listOf(randomNationalityCode()),
          religion = randomReligion(),
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
        .uri(libraApiUrl(personOne.cId))
        .authorised(listOf(Roles.API_READ_ONLY))
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
      Assertions.assertThat(responseBody.identifiers.cids).containsExactlyInAnyOrderElementsOf(
        listOf(
          personOne.cId,
        ),
      )
    }

    @Test
    fun `should return linked crn`() {
      val cId = randomCId()
      val crn = randomCrn()

      val person = createPersonWithNewKey(
        Person(
          firstName = randomName(),
          lastName = randomName(),
          middleNames = randomName(),
          dateOfBirth = randomDate(),
          sourceSystem = SourceSystemType.LIBRA,
          crn = crn,
          cId = cId,
        ),
      )

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(cId))
        .authorised(listOf(Roles.API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      println("responseBody = $responseBody")

      Assertions.assertThat(responseBody.identifiers.crns.size).isEqualTo(1)
      Assertions.assertThat(responseBody.identifiers.crns.first()).isEqualTo(crn)
    }
  }

  @Nested
  inner class ErrorScenarios {

    @Test
    fun `should return not found 404 with userMessage to show that the c_Id is not found`() {
      val nonExistentCId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
      val expectedErrorMessage = "Not found: $nonExistentCId"
      webTestClient.get()
        .uri(libraApiUrl(nonExistentCId))
        .authorised(listOf(Roles.API_READ_ONLY))
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
        .uri(libraApiUrl("accessdenied"))
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
        .uri(libraApiUrl("unauthorised"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun libraApiUrl(cId: String?) = "/person/libra/$cId"
}
