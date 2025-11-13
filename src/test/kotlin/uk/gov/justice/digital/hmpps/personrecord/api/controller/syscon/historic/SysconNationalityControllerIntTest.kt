package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonNationalityResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonNationalityRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.*
import java.util.UUID

class SysconNationalityControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonNationalityRepository: PrisonNationalityRepository

  @Nested
  inner class Creation {

    @Test
    fun `should store current nationality against a prison number`() {
      val prisonNumber = randomPrisonNumber()

      val currentCode = randomPrisonNationalityCode()
      val currentNationality = createRandomPrisonNationality(prisonNumber, currentCode, current = true)

      val currentCreationResponse = postNationality(currentNationality)
      assertCorrectValuesSaved(currentNationality, currentCreationResponse.cprNationalityId)

      val historicCode = randomPrisonNationalityCode()
      val historicNationality = createRandomPrisonNationality(prisonNumber, historicCode, current = false)

      val historicCreationResponse = postNationality(historicNationality)
      assertCorrectValuesSaved(historicNationality, historicCreationResponse.cprNationalityId)
    }
  }

  @Nested
  inner class Update {

    @Test
    fun `should update a nationality`() {
      val prisonNumber = randomPrisonNumber()

      val currentCode = randomPrisonNationalityCode()
      val currentNationality = createRandomPrisonNationality(prisonNumber, currentCode, current = true)

      val currentCreationResponse = postNationality(currentNationality)
      assertCorrectValuesSaved(currentNationality, currentCreationResponse.cprNationalityId)

      val updatedCode = randomPrisonNationalityCode()
      val updatedNationality = createRandomPrisonNationality(prisonNumber, updatedCode, current = false)

      webTestClient
        .put()
        .uri("/syscon-sync/nationality/${currentCreationResponse.cprNationalityId}")
        .bodyValue(updatedNationality)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isOk

      assertCorrectValuesSaved(updatedNationality, currentCreationResponse.cprNationalityId)
    }
  }

  @Test
  fun `should return Access Denied 403 when role is wrong`() {
    val expectedErrorMessage = "Forbidden: Access Denied"
    webTestClient.post()
      .uri("/syscon-sync/nationality")
      .bodyValue(createRandomPrisonNationality(randomPrisonNumber(), randomPrisonNationalityCode(), true))
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
      .uri("/syscon-sync/nationality")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  private fun postNationality(nationality: PrisonNationality): PrisonNationalityResponse {
    val nationalityResponse = webTestClient
      .post()
      .uri("/syscon-sync/nationality")
      .bodyValue(nationality)
      .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody(PrisonNationalityResponse::class.java)
      .returnResult()
      .responseBody!!

    return nationalityResponse
  }

  private fun assertCorrectValuesSaved(
    nationality: PrisonNationality,
    nationalityId: UUID,
  ) {
    val current = awaitNotNull { prisonNationalityRepository.findByCprNationalityId(nationalityId) }

    assertThat(current.nationalityCode).isEqualTo(NationalityCode.fromPrisonMapping(nationality.nationalityCode))
    assertThat(current.prisonNumber).isEqualTo(nationality.prisonNumber)
    assertThat(current.startDate).isEqualTo(nationality.startDate)
    assertThat(current.endDate).isEqualTo(nationality.endDate)
    assertThat(current.createUserId).isEqualTo(nationality.createUserId)
    assertThat(current.createDateTime).isEqualTo(nationality.createDateTime)
    assertThat(current.createDisplayName).isEqualTo(nationality.createDisplayName)
    assertThat(current.modifyDateTime).isEqualTo(nationality.modifyDateTime)
    assertThat(current.modifyUserId).isEqualTo(nationality.modifyUserId)
    assertThat(current.modifyDisplayName).isEqualTo(nationality.modifyDisplayName)
    assertThat(current.notes).isEqualTo(nationality.notes)
  }

  private fun createRandomPrisonNationality(prisonNumber: String, code: String, current: Boolean): PrisonNationality = PrisonNationality(
    prisonNumber = prisonNumber,
    nationalityCode = code,
    startDate = randomDate(),
    endDate = randomDate(),
    createUserId = randomName(),
    createDateTime = randomDateTime(),
    createDisplayName = randomName(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    modifyDisplayName = randomName(),
    current = current,
    notes = randomName(),
  )
}
