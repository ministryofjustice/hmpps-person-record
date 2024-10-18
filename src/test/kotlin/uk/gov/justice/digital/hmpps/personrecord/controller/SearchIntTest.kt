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
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class SearchIntTest : WebTestBase() {

  @Test
  fun `should return OFFENDER person record with one record`() {
    val crn = randomCRN()
    val personKeyEntity = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
      personKeyEntity = personKeyEntity,
    )

    val responseBody = webTestClient.get()
      .uri(searchOffenderUrl(crn))
      .authorised(listOf(Roles.ROLE_CORE_PERSON_RECORD_API__SEARCH__RO.name))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonIdentifierRecord::class.java)
      .returnResult()
      .responseBody!!

    checkTelemetry(TelemetryEventType.CPR_SEARCH_REQUEST, mapOf("CRN" to crn, "UUID" to personKeyEntity.personId.toString()))

    assertThat(responseBody.size).isEqualTo(1)
    assertThat(responseBody[0].id).isEqualTo(crn)
    assertThat(responseBody[0].sourceSystem).isEqualTo(DELIUS.name)
  }

  @Test
  fun `should return PRISONER person record with one record`() {
    val prisonNumber = randomPrisonNumber()
    val personKeyEntity = createPersonKey()
    createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        prisonNumber = prisonNumber,
        sourceSystemType = NOMIS,
      ),
      personKeyEntity = personKeyEntity,
    )

    val responseBody = webTestClient.get()
      .uri(searchPrisonerUrl(prisonNumber))
      .authorised(listOf(Roles.ROLE_CORE_PERSON_RECORD_API__SEARCH__RO.name))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonIdentifierRecord::class.java)
      .returnResult()
      .responseBody!!

    checkTelemetry(TelemetryEventType.CPR_SEARCH_REQUEST, mapOf("PRISON_NUMBER" to prisonNumber, "UUID" to personKeyEntity.personId.toString()))

    assertThat(responseBody.size).isEqualTo(1)
    assertThat(responseBody[0].id).isEqualTo(prisonNumber)
    assertThat(responseBody[0].sourceSystem).isEqualTo(NOMIS.name)
  }

  @Test
  fun `should return DEFENDANT person record with one record`() {
    val defendantId = randomDefendantId()
    val personKeyEntity = createPersonKey()
    createPerson(
      Person(
        defendantId = defendantId,
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = personKeyEntity,
    )

    val responseBody = webTestClient.get()
      .uri(searchDefendantUrl(defendantId))
      .authorised(listOf(Roles.ROLE_CORE_PERSON_RECORD_API__SEARCH__RO.name))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonIdentifierRecord::class.java)
      .returnResult()
      .responseBody!!

    checkTelemetry(TelemetryEventType.CPR_SEARCH_REQUEST, mapOf("DEFENDANT_ID" to defendantId, "UUID" to personKeyEntity.personId.toString()))

    assertThat(responseBody.size).isEqualTo(1)
    assertThat(responseBody[0].id).isEqualTo(defendantId)
    assertThat(responseBody[0].sourceSystem).isEqualTo(COMMON_PLATFORM.name)
  }

  @Test
  fun `should not return LIBRA person record that is linked`() {
    val crn = randomCRN()
    val personKeyEntity = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        sourceSystemType = LIBRA,
      ),
      personKeyEntity = personKeyEntity,
    )

    val responseBody = webTestClient.get()
      .uri(searchOffenderUrl(crn))
      .authorised(listOf(Roles.ROLE_CORE_PERSON_RECORD_API__SEARCH__RO.name))
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
  fun `should just return person record with a record with no UUID`() {
    val crn = randomCRN()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
    )

    val responseBody = webTestClient.get()
      .uri(searchOffenderUrl(crn))
      .authorised(listOf(Roles.ROLE_CORE_PERSON_RECORD_API__SEARCH__RO.name))
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
      .authorised(listOf(Roles.ROLE_CORE_PERSON_RECORD_API__SEARCH__RO.name))
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
        sourceSystemType = NOMIS,
      ),
      personKeyEntity = personKeyEntity,
    )
    val defendantId = UUID.randomUUID().toString()
    createPerson(
      Person(
        defendantId = defendantId,
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = personKeyEntity,
    )

    val responseBody = webTestClient.get()
      .uri(searchOffenderUrl(crn))
      .authorised(listOf(Roles.ROLE_CORE_PERSON_RECORD_API__SEARCH__RO.name))
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
      .authorised(listOf(Roles.ROLE_CORE_PERSON_RECORD_API__SEARCH__RO.name))
      .exchange()
      .expectStatus()
      .isNotFound
  }

  companion object {

    private fun searchOffenderUrl(crn: String) = "/search/offender/$crn"

    private fun searchPrisonerUrl(prisonerNumber: String) = "/search/prisoner/$prisonerNumber"

    private fun searchDefendantUrl(defendantId: String) = "/search/defendant/$defendantId"

    @JvmStatic
    fun searchUrls(): List<String> {
      return listOf(
        searchOffenderUrl(randomCRN()),
        searchPrisonerUrl(randomPrisonNumber()),
        searchDefendantUrl(randomDefendantId()),
      )
    }
  }
}
