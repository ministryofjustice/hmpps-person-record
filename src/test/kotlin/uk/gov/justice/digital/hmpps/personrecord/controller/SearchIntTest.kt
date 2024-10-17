package uk.gov.justice.digital.hmpps.personrecord.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class SearchIntTest : WebTestBase() {

  private fun searchUrl(personId: String) = "/api/search/$personId"

  @Test
  fun `should returns person record with one record`() {
    val crn = randomCRN()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
      personKeyEntity = createPersonKey(),
    )

    val responseBody = webTestClient.get()
      .uri(searchUrl(crn))
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
      .uri(searchUrl(crn))
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
      .uri(searchUrl(crn))
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
      .uri(searchUrl(crn))
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
      .uri(searchUrl(crn))
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

  @Test
  fun `should return FORBIDDEN with wrong role`() {
    webTestClient.get()
      .uri(searchUrl(randomCRN()))
      .authorised(listOf("WRONG_ROLE"))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should return UNAUTHORIZED without auth token`() {
    webTestClient.get()
      .uri(searchUrl(randomCRN()))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
