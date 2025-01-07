package uk.gov.justice.digital.hmpps.personrecord.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.PersonIdentifierRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SearchIntTest : WebTestBase() {

  @Test
  fun `should return OFFENDER person record with one record`() {
    val crn = randomCRN()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
      personKeyEntity = createPersonKey(),
    )

    val responseBody = webTestClient.get()
      .uri(searchOffenderUrl(crn))
      .authorised(listOf(Roles.SEARCH_API_READ_ONLY))
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
    createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        prisonNumber = prisonNumber,
        sourceSystem = NOMIS,
      ),
      personKeyEntity = createPersonKey(),
    )

    val responseBody = webTestClient.get()
      .uri(searchPrisonerUrl(prisonNumber))
      .authorised(listOf(Roles.SEARCH_API_READ_ONLY))
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
    createPerson(
      Person(
        defendantId = defendantId,
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = createPersonKey(),
    )

    val responseBody = webTestClient.get()
      .uri(searchDefendantUrl(defendantId))
      .authorised(listOf(Roles.SEARCH_API_READ_ONLY))
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
    createPerson(
      Person(
        defendantId = libraDefendantId,
        sourceSystem = LIBRA,
      ),
      personKeyEntity = createPersonKey(),
    )

    val responseBody = webTestClient.get()
      .uri(searchDefendantUrl(libraDefendantId))
      .authorised(listOf(Roles.SEARCH_API_READ_ONLY))
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
    val crn = randomCRN()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
    )

    val responseBody = webTestClient.get()
      .uri(searchOffenderUrl(crn))
      .authorised(listOf(Roles.SEARCH_API_READ_ONLY))
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
    val crn = randomCRN()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
    )

    val responseBody = webTestClient.get()
      .uri(searchOffenderUrl(crn))
      .authorised(listOf(Roles.SEARCH_API_READ_ONLY))
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
    val crn = randomCRN()
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
      .authorised(listOf(Roles.SEARCH_API_READ_ONLY))
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
      .authorised(listOf(Roles.SEARCH_API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isNotFound
  }

  companion object {

    private fun searchOffenderUrl(crn: String) = "/search/offender/$crn"

    private fun searchPrisonerUrl(prisonerNumber: String) = "/search/prisoner/$prisonerNumber"

    private fun searchDefendantUrl(defendantId: String) = "/search/defendant/$defendantId"

    @JvmStatic
    fun searchUrls(): List<String> = listOf(
      searchOffenderUrl(randomCRN()),
      searchPrisonerUrl(randomPrisonNumber()),
      searchDefendantUrl(randomDefendantId()),
    )
  }
}
