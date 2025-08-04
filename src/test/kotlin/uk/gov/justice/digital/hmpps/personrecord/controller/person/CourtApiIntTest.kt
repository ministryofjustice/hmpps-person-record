package uk.gov.justice.digital.hmpps.personrecord.controller.person

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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationality
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion

class CourtApiIntTest : WebTestBase() {

  @Nested
  inner class CommonPlatform

  @Test
  fun `should return ok for get`() {
    val firstName = randomName()
    val lastName = randomName()
    val middleNames = randomName()
    val title = "Mrs"
    val pnc = randomPnc()
    val noFixedAbode = true
    val startDate = randomDate()
    val endDate = randomDate()
    val postcode = randomPostcode()
    val nationality = randomNationality()
    val religion = randomReligion()
    val ethnicity = randomEthnicity()

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
        sourceSystem = COMMON_PLATFORM,
        title = randomName(),
        crn = crn,
        sexCode = SexCode.M,
        prisonNumber = prisonNumber,
        ethnicity = ethnicity,
        nationality = nationality,
        religion = religion,
        cId = cid,
        defendantId = defendantId,
        aliases = listOf(Alias(firstName = firstName, middleNames = middleNames, lastName = lastName, dateOfBirth = randomDate(), titleCode = TitleCode.from(title))),
        addresses = listOf(Address(noFixedAbode = noFixedAbode, startDate = startDate, endDate = endDate, postcode = postcode, buildingName = buildingName, buildingNumber = buildingNumber, thoroughfareName = thoroughfareName, dependentLocality = dependentLocality, postTown = postTown)),
        references = listOf(
          Reference(identifierType = IdentifierType.PNC, identifierValue = pnc),
          Reference(identifierType = IdentifierType.CRO, identifierValue = cro),
        ),

      ),
    )

    val responseBody = webTestClient.get()
      .uri(commonPLatformApiUrl(person))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java) // TODO different class
      .returnResult()
      .responseBody!!

    val canonicalAlias = CanonicalAlias(firstName = firstName, lastName = lastName, middleNames = middleNames, title = CanonicalTitle(code = "MRS", description = "Mrs"))
    val canonicalNationality = listOf(CanonicalNationality(code = nationality, description = nationality))
    val canonicalAddress = CanonicalAddress(noFixedAbode = noFixedAbode, startDate = startDate.toString(), endDate = endDate.toString(), postcode = postcode, buildingName = buildingName, buildingNumber = buildingNumber, thoroughfareName = thoroughfareName, dependentLocality = dependentLocality, postTown = postTown)
    val canonicalReligion = CanonicalReligion(code = religion, description = religion)
    val canonicalEthnicity = CanonicalEthnicity(code = ethnicity, description = ethnicity)

    assertThat(responseBody.firstName).isEqualTo(person.getPrimaryName().firstName)
    assertThat(responseBody.middleNames).isEqualTo(person.getPrimaryName().middleNames)
    assertThat(responseBody.lastName).isEqualTo(person.getPrimaryName().lastName)
    assertThat(responseBody.dateOfBirth).isEqualTo(person.getPrimaryName().dateOfBirth.toString())
    assertThat(responseBody.title.code).isEqualTo(person.getPrimaryName().titleCode?.code)
    assertThat(responseBody.title.description).isEqualTo(person.getPrimaryName().titleCode?.description)
    assertThat(responseBody.aliases.first().title.code).isEqualTo(person.getAliases().first().titleCode?.code)
    assertThat(responseBody.aliases.first().title.description).isEqualTo(person.getAliases().first().titleCode?.description)
    assertThat(responseBody.nationalities.first().code).isEqualTo(canonicalNationality.first().code)
    assertThat(responseBody.nationalities.first().description).isEqualTo(canonicalNationality.first().description)
    assertThat(responseBody.sex.code).isEqualTo("M")
    assertThat(responseBody.sex.description).isEqualTo("Male")
    assertThat(responseBody.religion.code).isEqualTo(canonicalReligion.code)
    assertThat(responseBody.religion.description).isEqualTo(canonicalReligion.description)
    assertThat(responseBody.ethnicity.code).isEqualTo(canonicalEthnicity.code)
    assertThat(responseBody.ethnicity.description).isEqualTo(canonicalEthnicity.description)
    assertThat(responseBody.aliases).isEqualTo(listOf(canonicalAlias))
    assertThat(responseBody.identifiers.cros).isEqualTo(listOf(cro)) // TODO not a list
    assertThat(responseBody.identifiers.pncs).isEqualTo(listOf(pnc))
    assertThat(responseBody.identifiers.crns).isEqualTo(listOf(crn))
    assertThat(responseBody.identifiers.defendantIds).isEqualTo(listOf(defendantId))
    assertThat(responseBody.identifiers.prisonNumbers).isEqualTo(listOf(prisonNumber))
    assertThat(responseBody.identifiers.cids).isEqualTo(listOf(cid))
    assertThat(responseBody.addresses).isEqualTo(listOf(canonicalAddress))
  }

  private fun commonPLatformApiUrl(person: PersonEntity) = "/person/commonplatform/${person.defendantId}"

  @Test
  fun `should return null when values are null or empty for get canonical record`() {
    val defendantId = randomDefendantId()

    val person = createPersonWithNewKey(
      Person(
        sourceSystem = COMMON_PLATFORM,
        defendantId = defendantId,
      ),
    )

    val responseBody = webTestClient.get()
      .uri(commonPLatformApiUrl(person))
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
    assertThat(responseBody.identifiers.defendantIds).isEqualTo(listOf(defendantId))
    assertThat(responseBody.identifiers.prisonNumbers).isEmpty()
    assertThat(responseBody.identifiers.cids).isEmpty()
    assertThat(responseBody.identifiers.pncs).isEmpty()
    assertThat(responseBody.identifiers.cros).isEmpty()
    assertThat(responseBody.identifiers.nationalInsuranceNumbers).isEmpty()
    assertThat(responseBody.identifiers.driverLicenseNumbers).isEmpty()
    assertThat(responseBody.identifiers.arrestSummonsNumbers).isEmpty()
  }

  @Test
  fun `should return when values are null or empty for get aliases`() {
    val defendantId = randomDefendantId()

    val aliasFirstName = randomName()

    val person = createPersonWithNewKey(
      Person(
        sourceSystem = COMMON_PLATFORM,
        defendantId = defendantId,
        aliases = listOf(Alias(firstName = aliasFirstName)),
      ),
    )

    val responseBody = webTestClient.get()
      .uri(commonPLatformApiUrl(person))
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
  }

  @Test
  fun `should return  when values are null or empty for get addresses`() {
    val defendantId = randomDefendantId()

    val postcode = randomPostcode()

    val person = createPersonWithNewKey(
      Person(
        sourceSystem = COMMON_PLATFORM,
        defendantId = defendantId,
        addresses = listOf(Address(postcode = postcode)),
      ),
    )

    val responseBody = webTestClient.get()
      .uri(commonPLatformApiUrl(person))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

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
    assertThat(responseBody.addresses.first().country).isNull()
    assertThat(responseBody.addresses.first().uprn).isNull()
  }

  @Test
  fun `should add list of additional identifiers to the canonical record`() {
    val personKey = createPersonKey()

    val personOneCro = randomCro()
    val personTwoCro = randomCro()

    val personOneCrn = randomCrn()
    val personTwoCrn = randomCrn()

    val personOnePnc = randomPnc()
    val personTwoPnc = randomPnc()

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
        title = randomName(),
        crn = personOneCrn,
        prisonNumber = randomPrisonNumber(),
        ethnicity = randomEthnicity(),
        nationality = randomNationality(),
        religion = randomReligion(),
        cId = randomCId(),
        defendantId = personOneDefendantId,
        masterDefendantId = personOneDefendantId,
        references = listOf(
          Reference(identifierType = IdentifierType.CRO, identifierValue = personOneCro),
          Reference(identifierType = IdentifierType.PNC, identifierValue = personOnePnc),
          Reference(identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER, identifierValue = personOneNationalInsuranceNumber),
          Reference(identifierType = IdentifierType.ARREST_SUMMONS_NUMBER, identifierValue = personOneArrestSummonNumber),
          Reference(identifierType = IdentifierType.DRIVER_LICENSE_NUMBER, identifierValue = personOneDriversLicenseNumber),
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
        title = randomName(),
        crn = personTwoCrn,
        prisonNumber = randomPrisonNumber(),
        ethnicity = randomEthnicity(),
        nationality = randomNationality(),
        religion = randomReligion(),
        cId = randomCId(),
        defendantId = personTwoDefendantId,
        masterDefendantId = personTwoDefendantId,
        references = listOf(
          Reference(identifierType = IdentifierType.CRO, identifierValue = personTwoCro),
          Reference(identifierType = IdentifierType.PNC, identifierValue = personTwoPnc),
          Reference(identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER, identifierValue = personTwoNationalInsuranceNumber),
          Reference(identifierType = IdentifierType.ARREST_SUMMONS_NUMBER, identifierValue = personTwoArrestSummonNumber),
          Reference(identifierType = IdentifierType.DRIVER_LICENSE_NUMBER, identifierValue = personTwoDriversLicenseNumber),
        ),
      ),
      personKey,
    )

    val responseBody = webTestClient.get()
      .uri(commonPLatformApiUrl(personOne))
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
    assertThat(responseBody.identifiers.crns).containsExactlyInAnyOrderElementsOf(listOf(personOne.crn, personTwo.crn))
    assertThat(responseBody.identifiers.defendantIds).containsExactlyInAnyOrderElementsOf(listOf(personOne.defendantId, personTwo.defendantId))
    assertThat(responseBody.identifiers.prisonNumbers).containsExactlyInAnyOrderElementsOf(listOf(personOne.prisonNumber, personTwo.prisonNumber))
    assertThat(responseBody.identifiers.cids).containsExactlyInAnyOrderElementsOf(listOf(personOne.cId, personTwo.cId))
  }

  @Test
  fun `should add an empty list of additional identifiers when null`() {
    val personKey = createPersonKey()

    val defendantId = randomDefendantId()

    val person = createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        middleNames = randomName(),
        dateOfBirth = randomDate(),
        sourceSystem = COMMON_PLATFORM,
        title = randomName(),
        ethnicity = randomEthnicity(),
        nationality = randomNationality(),
        religion = randomReligion(),
        masterDefendantId = randomDefendantId(),
        defendantId = defendantId,
      ),
      personKey,
    )

    val responseBody = webTestClient.get()
      .uri(commonPLatformApiUrl(person))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.identifiers.crns).isEmpty()
    assertThat(responseBody.identifiers.cids).isEmpty()
    assertThat(responseBody.identifiers.defendantIds).isNotEmpty()
    assertThat(responseBody.identifiers.prisonNumbers).isEmpty()
  }

  @Test
  fun `should return not found 404 with userMessage to show that the UUID is not found`() {
    val nonExistentDefendantId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    val expectedErrorMessage = "Not found: $nonExistentDefendantId"
    webTestClient.get()
      .uri("/person/commonplatform/$nonExistentDefendantId")
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
      .uri("/person/commonplatform/accessdenied")
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
      .uri("/person/commonplatform/unauthorised")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
