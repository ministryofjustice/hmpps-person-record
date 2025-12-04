package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonSexualOrientationRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexualOrientation

class SysconSexualOrientationControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonSexualOrientationRepository: PrisonSexualOrientationRepository

  @Nested
  inner class Creation {

    @Test
    fun `should save current sexual orientation against a prison number`() {
      val prisonNumber = randomPrisonNumber()

      val currentCode = randomPrisonSexualOrientation()
      val currentSexualOrientation = createRandomPrisonSexualOrientation(prisonNumber, currentCode, current = true)

      postSexualOrientation(prisonNumber, currentSexualOrientation)
      assertCorrectValuesSaved(prisonNumber, currentSexualOrientation)
    }
  }

  @Nested
  inner class Update {

    @Test
    fun `should update an existing sexual orientation`() {
      val prisonNumber = randomPrisonNumber()

      val currentCode = randomPrisonSexualOrientation()
      val currentSexualOrientation = createRandomPrisonSexualOrientation(prisonNumber, currentCode, current = true)

      postSexualOrientation(prisonNumber, currentSexualOrientation)
      assertCorrectValuesSaved(prisonNumber, currentSexualOrientation)

      val updatedCode = randomPrisonSexualOrientation()
      val updatedSexualOrientation = createRandomPrisonSexualOrientation(prisonNumber, updatedCode, current = true)

      postSexualOrientation(prisonNumber, updatedSexualOrientation)
      assertCorrectValuesSaved(prisonNumber, updatedSexualOrientation)
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.post()
        .uri("/syscon-sync/sexual-orientation/" + randomPrisonNumber())
        .bodyValue(createRandomPrisonSexualOrientation(randomPrisonNumber(), randomPrisonSexualOrientation(), true))
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
        .uri("/syscon-sync/sexual-orientation/" + randomPrisonNumber())
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
      .uri("/syscon-sync/sexual-orientation/$prisonNumber")
      .bodyValue(sexualOrientation)
      .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun assertCorrectValuesSaved(
    prisonNumber: String,
    sexualOrientation: PrisonSexualOrientation,
  ) {
    val current = awaitNotNull { prisonSexualOrientationRepository.findByPrisonNumber(prisonNumber) }

    assertThat(current.prisonNumber).isEqualTo(prisonNumber)
    assertThat(current.sexualOrientationCode).isEqualTo(SexualOrientation.from(sexualOrientation))
    assertThat(current.startDate).isEqualTo(sexualOrientation.startDate)
    assertThat(current.endDate).isEqualTo(sexualOrientation.endDate)
    assertThat(current.createUserId).isEqualTo(sexualOrientation.createUserId)
    assertThat(current.createDateTime).isEqualTo(sexualOrientation.createDateTime)
    assertThat(current.createDisplayName).isEqualTo(sexualOrientation.createDisplayName)
    assertThat(current.modifyDateTime).isEqualTo(sexualOrientation.modifyDateTime)
    assertThat(current.modifyUserId).isEqualTo(sexualOrientation.modifyUserId)
    assertThat(current.modifyDisplayName).isEqualTo(sexualOrientation.modifyDisplayName)
  }

  private fun createRandomPrisonSexualOrientation(prisonNumber: String, code: Map.Entry<String, SexualOrientation>, current: Boolean): PrisonSexualOrientation = PrisonSexualOrientation(
    prisonNumber = prisonNumber,
    sexualOrientationCode = code.key,
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
}
