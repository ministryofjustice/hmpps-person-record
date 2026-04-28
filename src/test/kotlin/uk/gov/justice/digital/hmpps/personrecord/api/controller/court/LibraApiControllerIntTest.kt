package uk.gov.justice.digital.hmpps.personrecord.api.controller.court

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalCountry
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonsNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn

class LibraApiControllerIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return when all fields populated`() {
      val firstName = randomName()
      val lastName = randomName()
      val middleNames = randomName()
      val title = randomTitleCode()

      val noFixedAbode = true
      val startDate = randomDate()
      val endDate = randomDate()
      val postcode = randomPostcode()
      val sex = randomCommonPlatformSexCode()

      val buildingName = randomName()
      val buildingNumber = randomBuildingNumber()
      val thoroughfareName = randomName()
      val dependentLocality = randomName()
      val postTown = randomName()
      val county = randomName()
      val countryCode = randomCountryCode()
      val uprn = randomUprn()
      val addressStatusCode = randomAddressStatusCode()
      val addressUsageCode = randomAddressUsageCode()
      val isActive = randomBoolean()
      val comment = randomName()

      val cro = randomCro()
      val pnc = randomLongPnc()
      val cid = randomCId()

      val person = createPersonWithNewKey(
        Person(
          firstName = randomName(),
          lastName = randomName(),
          middleNames = randomName(),
          dateOfBirth = randomDate(),
          sourceSystem = LIBRA,
          titleCode = title.value,

          sexCode = sex.value,

          cId = cid,

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
              county = county,
              countryCode = countryCode,
              uprn = uprn,
              statusCode = addressStatusCode,
              comment = comment,
              usages = listOf(AddressUsage(addressUsageCode, isActive)),
            ),
          ),
          references = listOf(
            Reference(
              identifierType = PNC,
              identifierValue = pnc,
            ),
            Reference(
              identifierType = CRO,
              identifierValue = cro,
            ),
          ),
        ),
      )

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(person.cId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!
      val canonicalAlias =
        CanonicalAlias(
          firstName = firstName,
          lastName = lastName,
          middleNames = middleNames,
          title = CanonicalTitle.from(title.value),
          sex = CanonicalSex.from(sex.value),
        )
      val canonicalAddress =
        CanonicalAddress(
          cprAddressId = person.addresses.first().updateId!!.toString(),
          noFixedAbode = noFixedAbode,
          startDate = startDate.toString(),
          endDate = endDate.toString(),
          postcode = postcode,
          buildingName = buildingName,
          buildingNumber = buildingNumber,
          thoroughfareName = thoroughfareName,
          dependentLocality = dependentLocality,
          postTown = postTown,
          county = county,
          country = CanonicalCountry.from(countryCode),
          uprn = uprn,
          status = CanonicalAddressStatus.from(addressStatusCode),
          comment = comment,
          usages = listOf(CanonicalAddressUsage(CanonicalAddressUsageCode.from(addressUsageCode), isActive)),
        )

      assertThat(responseBody.cprUUID).isNull()
      assertThat(responseBody.firstName).isEqualTo(person.getPrimaryName().firstName)
      assertThat(responseBody.middleNames).isEqualTo(person.getPrimaryName().middleNames)
      assertThat(responseBody.lastName).isEqualTo(person.getPrimaryName().lastName)
      assertThat(responseBody.dateOfBirth).isEqualTo(person.getPrimaryName().dateOfBirth.toString())
      assertThat(responseBody.title.code).isEqualTo(person.getPrimaryName().titleCode?.name)
      assertThat(responseBody.title.description).isEqualTo(person.getPrimaryName().titleCode?.description)
      assertThat(responseBody.aliases.first().title.code).isEqualTo(person.getAliases().first().titleCode?.name)
      assertThat(responseBody.aliases.first().title.description).isEqualTo(
        person.getAliases().first().titleCode?.description,
      )
      assertThat(responseBody.aliases.first().sex.code).isEqualTo(person.getAliases().first().sexCode?.name)
      assertThat(responseBody.aliases.first().sex.description).isEqualTo(
        person.getAliases().first().sexCode?.description,
      )
      assertThat(responseBody.sex.code).isEqualTo(sex.value.name)
      assertThat(responseBody.sex.description).isEqualTo(sex.value.description)
      assertThat(responseBody.aliases).isEqualTo(listOf(canonicalAlias))
      assertThat(responseBody.identifiers.cros).isEqualTo(listOf(cro))
      assertThat(responseBody.identifiers.pncs).isEqualTo(listOf(pnc))
      assertThat(responseBody.identifiers.cids).isEqualTo(listOf(cid))
      assertThat(responseBody.addresses).isEqualTo(listOf(canonicalAddress))
    }

    @Test
    fun `should return nulls when values are null or empty`() {
      val cId = randomCId()

      val person = createPersonWithNewKey(
        Person(
          sourceSystem = LIBRA,
          cId = cId,
        ),
      )

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(person.cId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      assertThat(responseBody.firstName).isNull()
      assertThat(responseBody.middleNames).isNull()
      assertThat(responseBody.lastName).isNull()
      assertThat(responseBody.dateOfBirth).isNull()
      assertThat(responseBody.title.code).isNull()
      assertThat(responseBody.title.description).isNull()
      assertThat(responseBody.ethnicity.code).isNull()
      assertThat(responseBody.ethnicity.description).isNull()
      assertThat(responseBody.sex.code).isNull()
      assertThat(responseBody.sex.description).isNull()
      assertThat(responseBody.religion.code).isNull()
      assertThat(responseBody.religion.description).isNull()
      assertThat(responseBody.title.code).isNull()
      assertThat(responseBody.title.description).isNull()
      assertThat(responseBody.ethnicity.code).isNull()
      assertThat(responseBody.ethnicity.description).isNull()
      assertThat(responseBody.sex.code).isNull()
      assertThat(responseBody.sex.description).isNull()
      assertThat(responseBody.religion.code).isNull()
      assertThat(responseBody.religion.description).isNull()
      assertThat(responseBody.nationalities).isEmpty()
      assertThat(responseBody.aliases).isEmpty()
      assertThat(responseBody.addresses).isEmpty()
      assertThat(responseBody.identifiers.crns).isEmpty()
      assertThat(responseBody.identifiers.defendantIds).isEmpty()
      assertThat(responseBody.identifiers.cids).isEqualTo(listOf(cId))
      assertThat(responseBody.identifiers.prisonNumbers).isEmpty()
      assertThat(responseBody.identifiers.pncs).isEmpty()
      assertThat(responseBody.identifiers.cros).isEmpty()
      assertThat(responseBody.identifiers.nationalInsuranceNumbers).isEmpty()
      assertThat(responseBody.identifiers.driverLicenseNumbers).isEmpty()
      assertThat(responseBody.identifiers.arrestSummonsNumbers).isEmpty()
    }

    @Test
    fun `should return nulls when some data (eg alias and address) values are null or empty`() {
      val cId = randomCId()

      val aliasFirstName = randomName()
      val postcode = randomPostcode()
      val person = createPersonWithNewKey(
        Person(
          sourceSystem = LIBRA,
          cId = cId,
          aliases = listOf(Alias(firstName = aliasFirstName)),
          addresses = listOf(
            Address(
              postcode = postcode,
            ),
          ),
        ),

      )

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(person.cId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      assertThat(responseBody.aliases.first().firstName).isEqualTo(aliasFirstName)
      assertThat(responseBody.aliases.first().lastName).isNull()
      assertThat(responseBody.aliases.first().middleNames).isNull()
      assertThat(responseBody.aliases.first().title.code).isNull()
      assertThat(responseBody.aliases.first().title.description).isNull()
      assertThat(responseBody.addresses.first().postcode).isEqualTo(postcode)
      assertThat(responseBody.addresses.first().startDate).isNull()
      assertThat(responseBody.addresses.first().endDate).isNull()
      assertThat(responseBody.addresses.first().noFixedAbode).isNull()
      assertThat(responseBody.addresses.first().buildingName).isNull()
      assertThat(responseBody.addresses.first().buildingNumber).isNull()
      assertThat(responseBody.addresses.first().thoroughfareName).isNull()
      assertThat(responseBody.addresses.first().dependentLocality).isNull()
      assertThat(responseBody.addresses.first().postTown).isNull()
      assertThat(responseBody.addresses.first().county).isNull()
      assertThat(responseBody.addresses.first().country).isNotNull()
      assertThat(responseBody.addresses.first().country.code).isNull()
      assertThat(responseBody.addresses.first().country.description).isNull()
      assertThat(responseBody.addresses.first().uprn).isNull()
      assertThat(responseBody.addresses.first().status).isNotNull()
      assertThat(responseBody.addresses.first().status.code).isNull()
      assertThat(responseBody.addresses.first().status.description).isNull()
      assertThat(responseBody.addresses.first().usages.size).isEqualTo(0)
    }

    @Test
    fun `should add list of additional identifiers off two people`() {
      val personOneCro = randomCro()
      val personTwoCro = randomCro()

      val personTwoCrn = randomCrn()

      val personOnePnc = randomLongPnc()
      val personTwoPnc = randomLongPnc()

      val personTwoNationalInsuranceNumber =
        randomNationalInsuranceNumber()

      val personTwoArrestSummonNumber =
        randomArrestSummonsNumber()

      val personTwoDriversLicenseNumber =
        randomDriverLicenseNumber()

      val personOneCId = randomCId()

      val personOne = createPerson(
        Person(
          firstName = randomName(),
          lastName = randomName(),
          middleNames = randomName(),
          dateOfBirth = randomDate(),
          sourceSystem = LIBRA,
          nationalities = listOf(randomNationalityCode()),
          religion = randomReligion(),

          cId = personOneCId,
          references = listOf(
            Reference(
              identifierType = CRO,
              identifierValue = personOneCro,
            ),
            Reference(
              identifierType = PNC,
              identifierValue = personOnePnc,
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
            Reference(
              identifierType = CRO,
              identifierValue = personTwoCro,
            ),
            Reference(
              identifierType = PNC,
              identifierValue = personTwoPnc,
            ),
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
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      assertThat(responseBody.identifiers.cros).containsExactly(personOneCro)
      assertThat(responseBody.identifiers.pncs).containsExactly(personOnePnc)
      assertThat(responseBody.identifiers.crns).containsExactlyInAnyOrderElementsOf(
        listOf(
          personTwo.crn,
        ),
      )
      assertThat(responseBody.identifiers.cids).containsExactlyInAnyOrderElementsOf(
        listOf(
          personOne.cId,
        ),
      )
    }
  }

  @Nested
  inner class ErrorScenarios {

    @Test
    fun `should return not found 404 with userMessage to show that the c_Id is not found`() {
      val nonExistentCId = "123456"
      val expectedErrorMessage = "Not found: $nonExistentCId"
      webTestClient.get()
        .uri(libraApiUrl(nonExistentCId))
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
