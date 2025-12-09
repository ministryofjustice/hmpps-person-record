package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonNationality
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonNationalityRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconNationalityControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonNationalityRepository: PrisonNationalityRepository

  @Nested
  inner class Creation {

    @Test
    fun `should save current nationality against a prison number`() {
      val prisonNumber = randomPrisonNumber()

      val currentCode = randomPrisonNationalityCode()
      val currentNationality = createRandomPrisonNationality(currentCode)

      postNationality(prisonNumber, currentNationality)
      assertCorrectValuesSaved(prisonNumber, currentNationality)
    }

    @Test
    fun `should save current nationality against a prison number when code is null`() {
      val prisonNumber = randomPrisonNumber()

      val currentNationality = createRandomPrisonNationality(null)

      postNationality(prisonNumber, currentNationality)
      assertCorrectValuesSaved(prisonNumber, currentNationality)
    }
  }

  @Nested
  inner class Update {

    @Test
    fun `should update an existing nationality`() {
      val prisonNumber = randomPrisonNumber()

      val currentCode = randomPrisonNationalityCode()
      val currentNationality = createRandomPrisonNationality(currentCode)

      postNationality(prisonNumber, currentNationality)
      assertCorrectValuesSaved(prisonNumber, currentNationality)

      val updatedCode = randomPrisonNationalityCode()
      val updatedNationality = createRandomPrisonNationality(updatedCode)

      postNationality(prisonNumber, updatedNationality)
      assertCorrectValuesSaved(prisonNumber, updatedNationality)
    }

    @Test
    fun `should update an existing nationality when code is null`() {
      val prisonNumber = randomPrisonNumber()

      val currentCode = randomPrisonNationalityCode()
      val currentNationality = createRandomPrisonNationality(currentCode)

      postNationality(prisonNumber, currentNationality)
      assertCorrectValuesSaved(prisonNumber, currentNationality)

      val updatedNationality = createRandomPrisonNationality(null)

      postNationality(prisonNumber, updatedNationality)
      assertCorrectValuesSaved(prisonNumber, updatedNationality)
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.post()
        .uri("/syscon-sync/nationality/" + randomPrisonNumber())
        .bodyValue(createRandomPrisonNationality(randomPrisonNationalityCode()))
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
        .uri("/syscon-sync/nationality/" + randomPrisonNumber())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun postNationality(prisonNumber: String, nationality: PrisonNationality) {
    webTestClient
      .post()
      .uri("/syscon-sync/nationality/$prisonNumber")
      .bodyValue(nationality)
      .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun assertCorrectValuesSaved(
    prisonNumber: String,
    nationality: PrisonNationality,
  ) {
    val current = awaitNotNull { prisonNationalityRepository.findByPrisonNumber(prisonNumber) }

    assertThat(current.prisonNumber).isEqualTo(prisonNumber)
    assertThat(current.nationalityCode).isEqualTo(NationalityCode.fromPrisonMapping(nationality.nationalityCode))
    assertThat(current.modifyDateTime).isEqualTo(nationality.modifyDateTime)
    assertThat(current.modifyUserId).isEqualTo(nationality.modifyUserId)
    assertThat(current.notes).isEqualTo(nationality.notes)
    assertThat(current.prisonRecordType).isEqualTo(PrisonRecordType.CURRENT)
  }

  private fun createRandomPrisonNationality(code: String?): PrisonNationality = PrisonNationality(
    nationalityCode = code,
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    notes = randomName(),
  )
}
