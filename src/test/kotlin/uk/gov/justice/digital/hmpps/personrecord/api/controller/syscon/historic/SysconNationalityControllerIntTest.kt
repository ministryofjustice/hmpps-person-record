package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonNationality
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconNationalityControllerIntTest : WebTestBase() {

  @Nested
  inner class Creation {

    @Test
    fun `should save current nationality against a prison number when code is sent`() {
      val prisonNumber = randomPrisonNumber()
      val currentNationality = createRandomPrisonNationality(NationalityCode.entries.random().toString())
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      postNationality(prisonNumber, currentNationality)
      assertCorrectValuesSaved(prisonNumber, currentNationality)
    }

    @Test
    fun `should delete nationality when a blank nationality code is sent`() {
      val prisonNumber = randomPrisonNumber()
      val currentNationality = createRandomPrisonNationality(" ")
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      postNationality(prisonNumber, currentNationality)

      val actualPerson = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      assertThat(actualPerson.nationalities.size).isEqualTo(0)
    }

    @Test
    fun `should delete nationality when a null nationality code is sent`() {
      val prisonNumber = randomPrisonNumber()
      val currentNationality = createRandomPrisonNationality(null)
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      postNationality(prisonNumber, currentNationality)

      val actualPerson = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      assertThat(actualPerson.nationalities.size).isEqualTo(0)
    }
  }

  @Nested
  inner class Update {

    @Test
    fun `should update an existing nationality`() {
      val prisonNumber = randomPrisonNumber()
      val currentCode = NationalityCode.entries.random().toString()
      val currentNationality = createRandomPrisonNationality(currentCode)
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      postNationality(prisonNumber, currentNationality)
      assertCorrectValuesSaved(prisonNumber, currentNationality)

      val updatedCode = NationalityCode.entries.random().toString()
      val updatedNationality = createRandomPrisonNationality(updatedCode)

      postNationality(prisonNumber, updatedNationality)
      assertCorrectValuesSaved(prisonNumber, updatedNationality)
    }

    @Test
    fun `should delete an existing nationality when a null code is sent`() {
      val prisonNumber = randomPrisonNumber()

      val currentCode = NationalityCode.entries.random().toString()
      val currentNationality = createRandomPrisonNationality(currentCode)
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      postNationality(prisonNumber, currentNationality)
      assertCorrectValuesSaved(prisonNumber, currentNationality)

      val updatedNationality = createRandomPrisonNationality(null)

      postNationality(prisonNumber, updatedNationality)

      val actualPerson = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      assertThat(actualPerson.nationalities.size).isEqualTo(0)
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
    val actualPerson = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }

    assertThat(actualPerson.prisonNumber).isEqualTo(prisonNumber)
    assertThat(actualPerson.nationalities.size).isEqualTo(1)
    val actualNationality = actualPerson.nationalities.first()
    val expectedNationality = if (!nationality.nationalityCode.isNullOrBlank()) nationality.nationalityCode else null

    expectedNationality?.let {
      assertThat(actualNationality.nationalityCode).isEqualTo(NationalityCode.valueOf(it))
    } ?: assertThat(actualNationality.nationalityCode).isEqualTo(expectedNationality)

    assertThat(actualNationality.person!!.id).isEqualTo(actualPerson.id)
  }

  private fun createRandomPrisonNationality(code: String?): PrisonNationality = PrisonNationality(
    nationalityCode = code,
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    notes = randomName(),
  )
}
