package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexualOrientation

class SysconSexualOrientationControllerIntTest : WebTestBase() {

  @Nested
  inner class Update {

    @Test
    fun `should update person sexual orientation`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val originalEntity = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      assertThat(originalEntity.sexualOrientation).isNull()

      val sexualOrientationCode = randomPrisonSexualOrientation()
      val prisonSexualOrientation = createRandomPrisonSexualOrientation(sexualOrientationCode.key)
      postSexualOrientation(prisonNumber, prisonSexualOrientation)

      assertCorrectValuesSaved(prisonNumber, originalEntity, sexualOrientationCode.value)
    }

    @Test
    fun `should update person sexual orientation to UNKNOWN when code is unknown`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val originalEntity = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      assertThat(originalEntity.sexualOrientation).isNull()

      val prisonSexualOrientation = createRandomPrisonSexualOrientation("ABC12345")
      postSexualOrientation(prisonNumber, prisonSexualOrientation)

      assertCorrectValuesSaved(prisonNumber, originalEntity, SexualOrientation.UNKNOWN)
    }

    @Test
    fun `should update person sexual orientation to null when code is null`() {
      val prisonNumber = randomPrisonNumber()
      val person = Person(
        prisonNumber = prisonNumber,
        firstName = randomName(),
        lastName = randomName(),
        dateOfBirth = randomDate(),
        sexualOrientation = randomPrisonSexualOrientation().value,
        sourceSystem = SourceSystemType.NOMIS,
      )
      createPerson(person)

      val originalEntity = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      assertThat(originalEntity.sexualOrientation).isNotNull()

      val sexualOrientation = createRandomPrisonSexualOrientation(null)
      postSexualOrientation(prisonNumber, sexualOrientation)

      assertCorrectValuesSaved(prisonNumber, originalEntity, null)
    }

    @Test
    fun `should return 404 not found when person with prison number does not exist`() {
      val prisonNumber = randomPrisonNumber()
      val expectedErrorMessage = "Not found: $prisonNumber"
      webTestClient.post()
        .uri(sexualOrientationUrl(prisonNumber))
        .bodyValue(createRandomPrisonSexualOrientation(randomPrisonSexualOrientation().key))
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.post()
        .uri(sexualOrientationUrl(randomPrisonNumber()))
        .bodyValue(createRandomPrisonSexualOrientation(randomPrisonSexualOrientation().key))
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
      webTestClient.post()
        .uri(sexualOrientationUrl(randomPrisonNumber()))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun postSexualOrientation(
    prisonNumber: String,
    sexualOrientation: PrisonSexualOrientation,
  ) {
    webTestClient
      .post()
      .uri(sexualOrientationUrl(prisonNumber))
      .bodyValue(sexualOrientation)
      .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun assertCorrectValuesSaved(
    prisonNumber: String,
    originalEntity: PersonEntity,
    sexualOrientation: SexualOrientation?,
  ) {
    awaitAssert {
      val updatedEntity = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(updatedEntity.sexualOrientation).isEqualTo(sexualOrientation)
      assertThat(updatedEntity.getPrimaryName().dateOfBirth).isEqualTo(originalEntity.getPrimaryName().dateOfBirth)
      assertThat(updatedEntity.getPrimaryName().firstName).isEqualTo(originalEntity.getPrimaryName().firstName)
      assertThat(updatedEntity.getPrimaryName().lastName).isEqualTo(originalEntity.getPrimaryName().lastName)
    }
  }

  private fun createRandomPrisonSexualOrientation(code: String?): PrisonSexualOrientation = PrisonSexualOrientation(
    sexualOrientationCode = code,
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
  )

  private fun sexualOrientationUrl(prisonNumber: String) = "/syscon-sync/sexual-orientation/$prisonNumber"
}
