package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonImmigrationStatus
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonImmigrationStatusRepository
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
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

      val immigrationStatus = createPrisonImmigrationStatus(prisonNumber, true)
      postImmigrationStatus(prisonNumber, immigrationStatus)

      awaitAssert {
        val updatedEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(updatedEntity.immigrationStatus).isEqualTo(immigrationStatus.interestToImmigration)
        assertThat(updatedEntity.getPrimaryName().dateOfBirth).isEqualTo(originalEntity.getPrimaryName().dateOfBirth)
        assertThat(updatedEntity.getPrimaryName().firstName).isEqualTo(originalEntity.getPrimaryName().firstName)
        assertThat(updatedEntity.getPrimaryName().lastName).isEqualTo(originalEntity.getPrimaryName().lastName)
      }
    }

    @Test
    fun `should return 404 not found when person with prison number does not exist`() {
      val prisonNumber = randomPrisonNumber()
      val expectedErrorMessage = "Not found: $prisonNumber"
      webTestClient.post()
        .uri(immigrationUrl(prisonNumber))
        .bodyValue(createPrisonImmigrationStatus(prisonNumber, randomBoolean()))
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
        .uri(immigrationUrl(randomPrisonNumber()))
        .bodyValue(createPrisonImmigrationStatus(randomPrisonNumber(), randomBoolean()))
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
        .uri(immigrationUrl(randomPrisonNumber()))
        .exchange()
        .expectStatus()
        .isUnauthorized
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

  private fun createPrisonImmigrationStatus(prisonNumber: String, current: Boolean): PrisonImmigrationStatus = PrisonImmigrationStatus(
    prisonNumber = prisonNumber,
    interestToImmigration = randomBoolean(),
    startDate = randomDate(),
    endDate = randomDate(),
    createUserId = randomName(),
    createDateTime = randomDateTime(),
    createDisplayName = randomName(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    modifyDisplayName = randomName(),
    current = current,
  )

  private fun immigrationUrl(prisonNumber: String) = "/syscon-sync/immigration-status/$prisonNumber"
}
