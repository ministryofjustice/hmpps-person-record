package uk.gov.justice.digital.hmpps.personrecord.controller.canonical

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Nationality
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonerNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion

class CanonicalApiIntTest : WebTestBase() {

  @Test
  fun `should return ok for get canonical record`() {
    val firstName = randomName()
    val lastName = randomName()
    val middleNames = randomName()
    val title = "Mr"
    val pnc = randomPnc()
    val noFixedAbode = true
    val startDate = randomDate()
    val endDate = randomDate()
    val postcode = randomPostcode()
    val nationality = randomNationalityCode()
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
        sourceSystem = NOMIS,
        titleCode = TitleCode.from(title),
        crn = crn,
        sexCode = SexCode.M,
        prisonNumber = prisonNumber,
        ethnicity = ethnicity,
        nationalities = listOf(Nationality(nationality)),
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
      .uri(canonicalAPIUrl(person.personKey?.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    val canonicalAlias = CanonicalAlias(firstName = firstName, lastName = lastName, middleNames = middleNames, title = CanonicalTitle(code = "MR", description = "Mr"))
    val canonicalNationality = nationality.getEntity()?.let { listOf(CanonicalNationality(it.code, it.description)) }
    val canonicalAddress = CanonicalAddress(noFixedAbode = noFixedAbode, startDate = startDate.toString(), endDate = endDate.toString(), postcode = postcode, buildingName = buildingName, buildingNumber = buildingNumber, thoroughfareName = thoroughfareName, dependentLocality = dependentLocality, postTown = postTown)
    val canonicalReligion = CanonicalReligion(code = religion, description = religion)
    val canonicalEthnicity = CanonicalEthnicity(code = ethnicity, description = ethnicity)

    assertThat(responseBody.cprUUID).isEqualTo(person.personKey?.personUUID.toString())
    assertThat(responseBody.firstName).isEqualTo(person.getPrimaryName().firstName)
    assertThat(responseBody.middleNames).isEqualTo(person.getPrimaryName().middleNames)
    assertThat(responseBody.lastName).isEqualTo(person.getPrimaryName().lastName)
    assertThat(responseBody.dateOfBirth).isEqualTo(person.getPrimaryName().dateOfBirth.toString())
    assertThat(responseBody.title.code).isEqualTo(person.getPrimaryName().titleCode?.code)
    assertThat(responseBody.title.description).isEqualTo(person.getPrimaryName().titleCode?.description)
    assertThat(responseBody.aliases.first().title.code).isEqualTo(person.getAliases().first().titleCode?.code)
    assertThat(responseBody.aliases.first().title.description).isEqualTo(person.getAliases().first().titleCode?.description)
    assertThat(responseBody.nationalities.first().code).isEqualTo(canonicalNationality?.first()?.code)
    assertThat(responseBody.nationalities.first().description).isEqualTo(canonicalNationality?.first()?.description)
    assertThat(responseBody.nationalities.first().startDate).isNull()
    assertThat(responseBody.nationalities.first().endDate).isNull()
    assertThat(responseBody.nationalities.first().notes).isNull()
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
    assertThat(responseBody.identifiers.defendantIds).isEqualTo(listOf(defendantId))
    assertThat(responseBody.identifiers.prisonNumbers).isEqualTo(listOf(prisonNumber))
    assertThat(responseBody.identifiers.cids).isEqualTo(listOf(cid))
    assertThat(responseBody.addresses).isEqualTo(listOf(canonicalAddress))
  }

  @Test
  fun `should return null when values are null or empty for get canonical record`() {
    val crn = randomCrn()

    val person = createPersonWithNewKey(
      Person(
        sourceSystem = NOMIS,
        crn = crn,
      ),
    )

    val responseBody = webTestClient.get()
      .uri(canonicalAPIUrl(person.personKey?.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.cprUUID).isEqualTo(person.personKey?.personUUID.toString())
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
    assertThat(responseBody.identifiers.crns).isEqualTo(listOf(crn))
    assertThat(responseBody.identifiers.defendantIds).isEmpty()
    assertThat(responseBody.identifiers.prisonNumbers).isEmpty()
    assertThat(responseBody.identifiers.cids).isEmpty()
    assertThat(responseBody.identifiers.pncs).isEmpty()
    assertThat(responseBody.identifiers.cros).isEmpty()
    assertThat(responseBody.identifiers.nationalInsuranceNumbers).isEmpty()
    assertThat(responseBody.identifiers.driverLicenseNumbers).isEmpty()
    assertThat(responseBody.identifiers.arrestSummonsNumbers).isEmpty()
  }

  @Test
  fun `should return when values are null or empty for get canonical record aliases`() {
    val crn = randomCrn()

    val aliasFirstName = randomName()

    val person = createPersonWithNewKey(
      Person(
        sourceSystem = NOMIS,
        crn = crn,
        aliases = listOf(Alias(firstName = aliasFirstName)),
      ),
    )

    val responseBody = webTestClient.get()
      .uri(canonicalAPIUrl(person.personKey?.personUUID.toString()))
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
  fun `should return  when values are null or empty for get canonical record addresses`() {
    val crn = randomCrn()

    val postcode = randomPostcode()

    val person = createPersonWithNewKey(
      Person(
        sourceSystem = NOMIS,
        crn = crn,
        addresses = listOf(Address(postcode = postcode)),
      ),
    )

    val responseBody = webTestClient.get()
      .uri(canonicalAPIUrl(person.personKey?.personUUID.toString()))
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
  fun `should return latest modified from 2 records`() {
    val personKey = createPersonKey()

    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), middleNames = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
      personKey,
    )

    val latestPerson = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), middleNames = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
      personKey,
    )

    val responseBody = webTestClient.get()
      .uri(canonicalAPIUrl(personKey.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.firstName).isEqualTo(latestPerson.getPrimaryName().firstName)
    assertThat(responseBody.middleNames).isEqualTo(latestPerson.getPrimaryName().middleNames)
    assertThat(responseBody.lastName).isEqualTo(latestPerson.getPrimaryName().lastName)
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

    val personOne = createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        middleNames = randomName(),
        dateOfBirth = randomDate(),
        sourceSystem = NOMIS,
        crn = personOneCrn,
        prisonNumber = randomPrisonNumber(),
        ethnicity = randomEthnicity(),
        nationality = randomPrisonerNationalityCode(),
        religion = randomReligion(),
        cId = randomCId(),
        defendantId = randomDefendantId(),
        masterDefendantId = randomDefendantId(),
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
        crn = personTwoCrn,
        prisonNumber = randomPrisonNumber(),
        ethnicity = randomEthnicity(),
        nationality = randomPrisonerNationalityCode(),
        religion = randomReligion(),
        cId = randomCId(),
        defendantId = randomDefendantId(),
        masterDefendantId = randomDefendantId(),
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
      .uri(canonicalAPIUrl(personKey.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.identifiers.cros).containsExactlyInAnyOrderElementsOf(listOf(personOneCro, personTwoCro))
    assertThat(responseBody.identifiers.pncs).containsExactlyInAnyOrderElementsOf(listOf(personOnePnc, personTwoPnc))
    assertThat(responseBody.identifiers.nationalInsuranceNumbers).containsExactlyInAnyOrderElementsOf(listOf(personOneNationalInsuranceNumber, personTwoNationalInsuranceNumber))
    assertThat(responseBody.identifiers.arrestSummonsNumbers).containsExactlyInAnyOrderElementsOf(listOf(personOneArrestSummonNumber, personTwoArrestSummonNumber))
    assertThat(responseBody.identifiers.driverLicenseNumbers).containsExactlyInAnyOrderElementsOf(listOf(personOneDriversLicenseNumber, personTwoDriversLicenseNumber))
    assertThat(responseBody.identifiers.crns).containsExactlyInAnyOrderElementsOf(listOf(personOne.crn, personTwo.crn))
    assertThat(responseBody.identifiers.defendantIds).containsExactlyInAnyOrderElementsOf(listOf(personOne.defendantId, personTwo.defendantId))
    assertThat(responseBody.identifiers.prisonNumbers).containsExactlyInAnyOrderElementsOf(listOf(personOne.prisonNumber, personTwo.prisonNumber))
    assertThat(responseBody.identifiers.cids).containsExactlyInAnyOrderElementsOf(listOf(personOne.cId, personTwo.cId))
  }

  @Test
  fun `should add an empty list of additional identifiers to the canonical record when null`() {
    val personKey = createPersonKey()

    createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        middleNames = randomName(),
        dateOfBirth = randomDate(),
        sourceSystem = NOMIS,
        ethnicity = randomEthnicity(),
        nationality = randomPrisonerNationalityCode(),
        religion = randomReligion(),
        masterDefendantId = randomDefendantId(),
      ),
      personKey,
    )

    val responseBody = webTestClient.get()
      .uri(canonicalAPIUrl(personKey.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.identifiers.crns).isEmpty()
    assertThat(responseBody.identifiers.cids).isEmpty()
    assertThat(responseBody.identifiers.defendantIds).isEmpty()
    assertThat(responseBody.identifiers.prisonNumbers).isEmpty()
  }

  @Test
  fun `should return latest modified with latest person set to null record`() {
    val personKey = createPersonKey()

    val person = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), middleNames = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
      personKey,
    )

    val latestPerson = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), middleNames = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
      personKey,
    )

    latestPerson.lastModified = null
    personRepository.saveAndFlush(latestPerson)

    val responseBody = webTestClient.get()
      .uri(canonicalAPIUrl(personKey.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.firstName).isEqualTo(person.getPrimaryName().firstName)
    assertThat(responseBody.middleNames).isEqualTo(person.getPrimaryName().middleNames)
    assertThat(responseBody.lastName).isEqualTo(person.getPrimaryName().lastName)
  }

  @Test
  fun `should return record when all last modified are null`() {
    val personKey = createPersonKey()

    val person = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), middleNames = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
      personKey,
    )

    val latestPerson = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), middleNames = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
      personKey,
    )

    latestPerson.lastModified = null
    person.lastModified = null
    personRepository.saveAndFlush(latestPerson)
    personRepository.saveAndFlush(person)

    val responseBody = webTestClient.get()
      .uri(canonicalAPIUrl(personKey.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody).isNotNull()
  }

  @Test
  fun `should redirect to merged to record`() {
    val sourcePersonFirstName = randomName()
    val targetPersonFirstName = randomName()

    val sourcePersonKey = createPersonKey()
    val targetPersonKey = createPersonKey()

    createPerson(
      Person.from(ProbationCase(name = Name(firstName = sourcePersonFirstName), identifiers = Identifiers())),
      personKeyEntity = sourcePersonKey,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = targetPersonFirstName), identifiers = Identifiers())),
      personKeyEntity = targetPersonKey,
    )

    mergeUuid(sourcePersonKey, targetPersonKey)

    webTestClient.get()
      .uri(canonicalAPIUrl(sourcePersonKey.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isEqualTo(301)
      .expectHeader()
      .valueEquals("Location", "/person/${targetPersonKey.personUUID}")
  }

  @Test
  fun `should redirect to the top node of a merged to record`() {
    val sourcePersonFirstName = randomName()
    val targetPersonFirstName = randomName()
    val newTargetPersonFirstName = randomName()

    val sourcePersonKey = createPersonKey()
    val targetPersonKey = createPersonKey()
    val newTargetPersonKey = createPersonKey()

    createPerson(
      Person.from(ProbationCase(name = Name(firstName = sourcePersonFirstName), identifiers = Identifiers())),
      personKeyEntity = sourcePersonKey,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = targetPersonFirstName), identifiers = Identifiers())),
      personKeyEntity = targetPersonKey,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = newTargetPersonFirstName), identifiers = Identifiers())),
      personKeyEntity = newTargetPersonKey,
    )

    mergeUuid(sourcePersonKey, targetPersonKey)
    mergeUuid(targetPersonKey, newTargetPersonKey)

    webTestClient.get()
      .uri(canonicalAPIUrl(sourcePersonKey.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isEqualTo(301)
      .expectHeader()
      .valueEquals("Location", "/person/${newTargetPersonKey.personUUID}")
  }

  @Test
  fun `should return bad request if canonical record is invalid uuid`() {
    val randomString = randomName()
    webTestClient.get()
      .uri(canonicalAPIUrl(randomString))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `should return not found 404 with userMessage to show that the UUID is not found`() {
    val randomUUId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    val expectedErrorMessage = "Not found: $randomUUId"
    webTestClient.get()
      .uri(canonicalAPIUrl(randomUUId))
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
      .uri(canonicalAPIUrl("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
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
      .uri(canonicalAPIUrl("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  private fun canonicalAPIUrl(uuid: String) = "/person/$uuid"
}
