package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonImmigrationStatus
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconImmigrationStatusControllerIntTest : WebTestBase() {

  @Nested
  inner class Update {

    @Test
    fun `should update person immigration status`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val originalEntity = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      assertThat(originalEntity.immigrationStatus).isNull()

      val immigrationStatus = createPrisonImmigrationStatus()
      postImmigrationStatus(prisonNumber, immigrationStatus)

      assertCorrectValuesSaved(prisonNumber, originalEntity, immigrationStatus.interestToImmigration)
    }

    @Test
    fun `should return 404 not found when person with prison number does not exist`() {
      val prisonNumber = randomPrisonNumber()
      val expectedErrorMessage = "Not found: $prisonNumber"
      webTestClient.post()
        .uri(immigrationUrl(prisonNumber))
        .bodyValue(createPrisonImmigrationStatus())
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
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val originalEntity = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      assertThat(originalEntity.immigrationStatus).isNull()

      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.post()
        .uri(immigrationUrl(prisonNumber))
        .bodyValue(createPrisonImmigrationStatus())
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)

      assertCorrectValuesSaved(prisonNumber, originalEntity, null)
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val originalEntity = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      assertThat(originalEntity.immigrationStatus).isNull()

      webTestClient.post()
        .uri(immigrationUrl(randomPrisonNumber()))
        .bodyValue(createPrisonImmigrationStatus())
        .exchange()
        .expectStatus()
        .isUnauthorized

      assertCorrectValuesSaved(prisonNumber, originalEntity, null)
    }
  }

  private fun postImmigrationStatus(prisonNumber: String, immigrationStatus: Any) {
    webTestClient
      .post()
      .uri(immigrationUrl(prisonNumber))
      .bodyValue(immigrationStatus)
      .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun assertCorrectValuesSaved(prisonNumber: String, originalEntity: PersonEntity, expectedImmigrationStatus: Boolean?) {
    val updatedEntity = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
    assertThat(updatedEntity.immigrationStatus).isEqualTo(expectedImmigrationStatus)
    assertThat(updatedEntity.getPrimaryName().dateOfBirth).isEqualTo(originalEntity.getPrimaryName().dateOfBirth)
    assertThat(updatedEntity.getPrimaryName().firstName).isEqualTo(originalEntity.getPrimaryName().firstName)
    assertThat(updatedEntity.getPrimaryName().lastName).isEqualTo(originalEntity.getPrimaryName().lastName)
  }

  private fun createPrisonImmigrationStatus(): PrisonImmigrationStatus = PrisonImmigrationStatus(
    interestToImmigration = randomBoolean(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
  )

  private fun immigrationUrl(prisonNumber: String) = "/syscon-sync/immigration-status/$prisonNumber"
}
