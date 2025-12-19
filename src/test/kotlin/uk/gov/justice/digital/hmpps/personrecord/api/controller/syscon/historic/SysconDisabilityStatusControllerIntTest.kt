package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatus
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconDisabilityStatusControllerIntTest : WebTestBase() {

  @Nested
  inner class Update {

    @Test
    fun `should update person disability status`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val originalEntity = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      assertThat(originalEntity.disability).isNull()

      val disabilityStatus = createPrisonDisabilityStatus(true)
      postDisabilityStatus(prisonNumber, disabilityStatus)

      awaitAssert {
        val updatedEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(updatedEntity.disability).isEqualTo(disabilityStatus.disability)
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
        .uri(disabilityUrl(prisonNumber))
        .bodyValue(createPrisonDisabilityStatus(randomBoolean()))
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
        .uri(disabilityUrl(randomPrisonNumber()))
        .bodyValue(createPrisonDisabilityStatus(randomBoolean()))
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
        .uri(disabilityUrl(randomPrisonNumber()))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun postDisabilityStatus(prisonNumber: String, disabilityStatus: PrisonDisabilityStatus) {
    webTestClient
      .post()
      .uri(disabilityUrl(prisonNumber))
      .bodyValue(disabilityStatus)
      .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun createPrisonDisabilityStatus(status: Boolean): PrisonDisabilityStatus = PrisonDisabilityStatus(
    disability = status,
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
  )

  private fun disabilityUrl(prisonNumber: String) = "/syscon-sync/disability-status/$prisonNumber"
}
