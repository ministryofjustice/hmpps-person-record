package uk.gov.justice.digital.hmpps.personrecord.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.SEARCH_API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.PersonIdentifierRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifierType.CRN
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifierType.C_ID
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifierType.DEFENDANT_ID
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifierType.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationality
import uk.gov.justice.digital.hmpps.personrecord.test.randomPersonId
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion

class SearchIntTest : WebTestBase() {

  @Test
  fun `should return OFFENDER person record with one record`() {
    val crn = randomCrn()
    createPersonWithNewKey(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
    )

    val responseBody = webTestClient.get()
      .uri(searchOffenderUrl(crn))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonIdentifierRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.size).isEqualTo(1)
    assertThat(responseBody[0].id).isEqualTo(crn)
    assertThat(responseBody[0].sourceSystem).isEqualTo(DELIUS.name)
  }

  @Test
  fun `should return PRISONER person record with one record`() {
    val prisonNumber = randomPrisonNumber()
    createPersonWithNewKey(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        prisonNumber = prisonNumber,
        sourceSystem = NOMIS,
      ),
    )

    val responseBody = webTestClient.get()
      .uri(searchPrisonerUrl(prisonNumber))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonIdentifierRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.size).isEqualTo(1)
    assertThat(responseBody[0].id).isEqualTo(prisonNumber)
    assertThat(responseBody[0].sourceSystem).isEqualTo(NOMIS.name)
  }

  @Test
  fun `should return DEFENDANT person record with one record`() {
    val defendantId = randomDefendantId()
    createPersonWithNewKey(
      Person(
        defendantId = defendantId,
        sourceSystem = COMMON_PLATFORM,
      ),
    )

    val responseBody = webTestClient.get()
      .uri(searchDefendantUrl(defendantId))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonIdentifierRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.size).isEqualTo(1)
    assertThat(responseBody[0].id).isEqualTo(defendantId)
    assertThat(responseBody[0].sourceSystem).isEqualTo(COMMON_PLATFORM.name)
  }

  @Test
  fun `should return LIBRA person record with one record`() {
    val libraDefendantId = randomDefendantId()
    createPersonWithNewKey(
      Person(
        defendantId = libraDefendantId,
        sourceSystem = LIBRA,
      ),
    )

    val responseBody = webTestClient.get()
      .uri(searchDefendantUrl(libraDefendantId))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonIdentifierRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.size).isEqualTo(1)
    assertThat(responseBody[0].id).isEqualTo(libraDefendantId)
    assertThat(responseBody[0].sourceSystem).isEqualTo(LIBRA.name)
  }

  @Test
  fun `should just return person record with a record with no UUID`() {
    val crn = randomCrn()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
    )

    val responseBody = webTestClient.get()
      .uri(searchOffenderUrl(crn))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonIdentifierRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.size).isEqualTo(1)
    assertThat(responseBody[0].id).isEqualTo(crn)
    assertThat(responseBody[0].sourceSystem).isEqualTo(DELIUS.name)
  }

  @Test
  fun `should return searching record not others`() {
    val crn = randomCrn()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
    )

    val responseBody = webTestClient.get()
      .uri(searchOffenderUrl(crn))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonIdentifierRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.size).isEqualTo(1)
    assertThat(responseBody[0].id).isEqualTo(crn)
    assertThat(responseBody[0].sourceSystem).isEqualTo(DELIUS.name)
  }

  @Test
  fun `should returns person record with multiple linked records`() {
    val crn = randomCrn()
    val personKeyEntity = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
      personKeyEntity = personKeyEntity,
    )
    val prisonNumber = randomPrisonNumber()
    createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        prisonNumber = prisonNumber,
        sourceSystem = NOMIS,
      ),
      personKeyEntity = personKeyEntity,
    )
    val defendantId = randomDefendantId()
    createPerson(
      Person(
        defendantId = defendantId,
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = personKeyEntity,
    )

    val responseBody = webTestClient.get()
      .uri(searchOffenderUrl(crn))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonIdentifierRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.size).isEqualTo(3)
    assertThat(responseBody[0].id).isEqualTo(crn)
    assertThat(responseBody[0].sourceSystem).isEqualTo(DELIUS.name)
    assertThat(responseBody[1].id).isEqualTo(prisonNumber)
    assertThat(responseBody[1].sourceSystem).isEqualTo(NOMIS.name)
    assertThat(responseBody[2].id).isEqualTo(defendantId)
    assertThat(responseBody[2].sourceSystem).isEqualTo(COMMON_PLATFORM.name)
  }

  @ParameterizedTest
  @MethodSource("searchUrls")
  fun `should return FORBIDDEN with wrong role`(url: String) {
    webTestClient.get()
      .uri(url)
      .authorised(listOf("WRONG_ROLE"))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @ParameterizedTest
  @MethodSource("searchUrls")
  fun `should return UNAUTHORIZED without auth token`(url: String) {
    webTestClient.get()
      .uri(url)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @MethodSource("searchUrls")
  fun `should return NOT FOUND when empty personId`(url: String) {
    webTestClient.get()
      .uri(url)
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `should return ok for get canonical record`() {
    val firstName = randomName()
    val lastName = randomName()
    val middleNames = randomName()
    val title = randomName()
    val pnc = randomPnc()
    val noFixAbode = true
    val startDate = randomDate()
    val endDate = randomDate()
    val postcode = randomPostcode()
    val nationality = randomNationality()

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
        middleNames = listOf(randomName()),
        dateOfBirth = randomDate(),
        sourceSystem = NOMIS,
        title = randomName(),
        crn = crn,
        prisonNumber = prisonNumber,
        ethnicity = randomEthnicity(),
        nationality = nationality,
        religion = randomReligion(),
        cId = cid,
        defendantId = defendantId,
        masterDefendantId = randomDefendantId(),
        aliases = listOf(Alias(firstName = firstName, middleNames = middleNames, lastName = lastName, dateOfBirth = randomDate(), title = title)),
        addresses = listOf(Address(noFixedAbode = noFixAbode, startDate = startDate, endDate = endDate, postcode = postcode, buildingName = buildingName, buildingNumber = buildingNumber, thoroughfareName = thoroughfareName, dependentLocality = dependentLocality, postTown = postTown)),
        references = listOf(
          Reference(identifierType = IdentifierType.PNC, identifierValue = pnc),
          Reference(identifierType = IdentifierType.CRO, identifierValue = cro),
        ),

      ),
    )

    val responseBody = webTestClient.get()
      .uri(searchForPerson(person.personKey?.personId.toString()))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    val canonicalAlias = CanonicalAlias(firstName = firstName, lastName = lastName, middleNames = middleNames, title = title)
    val canonicalIdentifiers = listOf(
      CanonicalIdentifier(CRO, listOf(cro)),
      CanonicalIdentifier(PNC, listOf(pnc)),
      CanonicalIdentifier(CRN, listOf(crn)),
      CanonicalIdentifier(DEFENDANT_ID, listOf(defendantId)),
      CanonicalIdentifier(PRISON_NUMBER, listOf(prisonNumber)),
      CanonicalIdentifier(C_ID, listOf(cid)),
    )

    val canonicalNationality = listOf(CanonicalNationality(nationalityCode = nationality))
    val canonicalAddress = CanonicalAddress(noFixedAbode = noFixAbode.toString(), startDate = startDate.toString(), endDate = endDate.toString(), postcode = postcode, buildingName = buildingName, buildingNumber = buildingNumber, thoroughfareName = thoroughfareName, dependentLocality = dependentLocality, postTown = postTown)

    assertThat(responseBody.cprUUID).isEqualTo(person.personKey?.personId.toString())
    assertThat(responseBody.firstName).isEqualTo(person.firstName)
    assertThat(responseBody.middleNames).isEqualTo(person.middleNames)
    assertThat(responseBody.lastName).isEqualTo(person.lastName)
    assertThat(responseBody.dateOfBirth).isEqualTo(person.dateOfBirth.toString())
    assertThat(responseBody.title).isEqualTo(person.title)
    assertThat(responseBody.ethnicity).isEqualTo(person.ethnicity)
    assertThat(responseBody.nationalities).isEqualTo(canonicalNationality)
    assertThat(responseBody.sex).isEqualTo(person.sex)
    assertThat(responseBody.religion).isEqualTo(person.religion)
    assertThat(responseBody.masterDefendantId).isEqualTo(person.masterDefendantId)
    assertThat(responseBody.aliases).isEqualTo(listOf(canonicalAlias))
    assertThat(responseBody.identifiers).containsExactlyInAnyOrderElementsOf(canonicalIdentifiers)
    assertThat(responseBody.addresses).isEqualTo(listOf(canonicalAddress))
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
      .uri(searchForPerson(personKey.personId.toString()))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.firstName).isEqualTo(latestPerson.firstName)
    assertThat(responseBody.middleNames).isEqualTo(latestPerson.middleNames)
    assertThat(responseBody.lastName).isEqualTo(latestPerson.lastName)
  }

  @Test
  fun `should add list of additional identifiers to the canonical record`() {
    val personKey = createPersonKey()

    val personOne = createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        middleNames = listOf(randomName()),
        dateOfBirth = randomDate(),
        sourceSystem = NOMIS,
        title = randomName(),
        crn = randomCrn(),
        prisonNumber = randomPrisonNumber(),
        ethnicity = randomEthnicity(),
        nationality = randomNationality(),
        religion = randomReligion(),
        cId = randomCId(),
        defendantId = randomDefendantId(),
        masterDefendantId = randomDefendantId(),
      ),
      personKey,
    )

    val personTwo = createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        middleNames = listOf(randomName()),
        dateOfBirth = randomDate(),
        sourceSystem = NOMIS,
        title = randomName(),
        crn = randomCrn(),
        prisonNumber = randomPrisonNumber(),
        ethnicity = randomEthnicity(),
        nationality = randomNationality(),
        religion = randomReligion(),
        cId = randomCId(),
        defendantId = randomDefendantId(),
        masterDefendantId = randomDefendantId(),
      ),
      personKey,
    )

    val responseBody = webTestClient.get()
      .uri(searchForPerson(personKey.personId.toString()))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.identifiers).isEqualTo(
      listOf(
        CanonicalIdentifier(CRN, listOf(personOne.crn, personTwo.crn)),
        CanonicalIdentifier(DEFENDANT_ID, listOf(personOne.defendantId, personTwo.defendantId)),
        CanonicalIdentifier(PRISON_NUMBER, listOf(personOne.prisonNumber, personTwo.prisonNumber)),
        CanonicalIdentifier(C_ID, listOf(personOne.cId, personTwo.cId)),
      ),
    )
  }

  @Test
  fun `should add an empty list of additional identifiers to the canonical record when null`() {
    val personKey = createPersonKey()

    createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        middleNames = listOf(randomName()),
        dateOfBirth = randomDate(),
        sourceSystem = NOMIS,
        title = randomName(),
        ethnicity = randomEthnicity(),
        nationality = randomNationality(),
        religion = randomReligion(),
        masterDefendantId = randomDefendantId(),
      ),
      personKey,
    )

    val responseBody = webTestClient.get()
      .uri(searchForPerson(personKey.personId.toString()))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    val expectedIdentifiers = listOf(
      CanonicalIdentifier(CRN, emptyList()),
      CanonicalIdentifier(C_ID, emptyList()),
      CanonicalIdentifier(DEFENDANT_ID, emptyList()),
      CanonicalIdentifier(PRISON_NUMBER, emptyList()),
    )
    assertThat(responseBody.identifiers).containsExactlyInAnyOrderElementsOf(expectedIdentifiers)
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
      .uri(searchForPerson(personKey.personId.toString()))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.firstName).isEqualTo(person.firstName)
    assertThat(responseBody.middleNames).isEqualTo(person.middleNames)
    assertThat(responseBody.lastName).isEqualTo(person.lastName)
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
      .uri(searchForPerson(personKey.personId.toString()))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody).isNotNull()
  }

  @Test
  fun `should return bad request if canonical record is invalid uuid`() {
    val randomString = randomName()
    webTestClient.get()
      .uri(searchForPerson(randomString))
      .authorised(listOf(SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  companion object {

    private fun searchForPerson(uuid: String) = "/search/person/$uuid"

    private fun searchOffenderUrl(crn: String) = "/search/offender/$crn"

    private fun searchPrisonerUrl(prisonerNumber: String) = "/search/prisoner/$prisonerNumber"

    private fun searchDefendantUrl(defendantId: String) = "/search/defendant/$defendantId"

    @JvmStatic
    fun searchUrls(): List<String> = listOf(
      searchOffenderUrl(randomCrn()),
      searchPrisonerUrl(randomPrisonNumber()),
      searchDefendantUrl(randomDefendantId()),
      searchForPerson(randomPersonId()),

    )
  }
}
