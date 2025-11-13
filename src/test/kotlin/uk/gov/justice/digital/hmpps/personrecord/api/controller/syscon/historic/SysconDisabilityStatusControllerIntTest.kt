package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatusResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonDisabilityStatusRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomDisability
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class SysconDisabilityStatusControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonDisabilityStatusRepository: PrisonDisabilityStatusRepository

  @Nested
  inner class Create {

    @Test
    fun `should store current and historic disability status against a prison number`() {
      val prisonNumber = randomPrisonNumber()

      val currentDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, randomDisability(), true)
      val currentCreationResponse = postDisabilityStatus(currentDisabilityStatus)

      assertCorrectValuesSaved(currentDisabilityStatus, currentCreationResponse.cprDisabilityStatusId)

      val historicDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, randomDisability(), false)
      val historicCreationResponse = postDisabilityStatus(historicDisabilityStatus)

      assertCorrectValuesSaved(historicDisabilityStatus, historicCreationResponse.cprDisabilityStatusId)
    }
  }

  @Nested
  inner class Update {

    @Test
    fun `should update a disability status`() {
      val prisonNumber = randomPrisonNumber()

      val currentDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, randomDisability(), true)
      val currentCreationResponse = postDisabilityStatus(currentDisabilityStatus)

      assertCorrectValuesSaved(currentDisabilityStatus, currentCreationResponse.cprDisabilityStatusId)

      val updatedDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, randomDisability(), false)

      webTestClient
        .put()
        .uri("/syscon-sync/disability-status/${currentCreationResponse.cprDisabilityStatusId}")
        .bodyValue(updatedDisabilityStatus)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isOk

      assertCorrectValuesSaved(updatedDisabilityStatus, currentCreationResponse.cprDisabilityStatusId)
    }
  }

  @Test
  fun `should return Access Denied 403 when role is wrong`() {
    val expectedErrorMessage = "Forbidden: Access Denied"
    webTestClient.post()
      .uri("/syscon-sync/disability-status")
      .bodyValue(createRandomPrisonDisabilityStatus(randomPrisonNumber(), randomDisability(), true))
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
      .uri("/syscon-sync/disability-status")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  private fun postDisabilityStatus(status: PrisonDisabilityStatus): PrisonDisabilityStatusResponse {
    val currentCreationResponse = webTestClient
      .post()
      .uri("/syscon-sync/disability-status")
      .bodyValue(status)
      .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody(PrisonDisabilityStatusResponse::class.java)
      .returnResult()
      .responseBody!!

    return currentCreationResponse
  }

  private fun createRandomPrisonDisabilityStatus(prisonNumber: String, status: Boolean, current: Boolean): PrisonDisabilityStatus = PrisonDisabilityStatus(
    prisonNumber = prisonNumber,
    disability = status,
    current = current,
    startDate = randomDate(),
    endDate = randomDate(),
    createUserId = randomName(),
    createDateTime = randomDateTime(),
    createDisplayName = randomName(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    modifyDisplayName = randomName(),
  )

  private fun assertCorrectValuesSaved(
    disabilityStatus: PrisonDisabilityStatus,
    statusId: UUID,
  ) {
    val status = awaitNotNull { prisonDisabilityStatusRepository.findByCprDisabilityStatusId(statusId) }

    assertThat(status.prisonNumber).isEqualTo(disabilityStatus.prisonNumber)
    assertThat(status.disability).isEqualTo(disabilityStatus.disability)
    assertThat(status.prisonRecordType).isEqualTo(PrisonRecordType.from(disabilityStatus.current))
    assertThat(status.startDate).isEqualTo(disabilityStatus.startDate)
    assertThat(status.endDate).isEqualTo(disabilityStatus.endDate)
    assertThat(status.createUserId).isEqualTo(disabilityStatus.createUserId)
    assertThat(status.createDateTime).isEqualTo(disabilityStatus.createDateTime)
    assertThat(status.createDisplayName).isEqualTo(disabilityStatus.createDisplayName)
    assertThat(status.modifyDateTime).isEqualTo(disabilityStatus.modifyDateTime)
    assertThat(status.modifyUserId).isEqualTo(disabilityStatus.modifyUserId)
    assertThat(status.modifyDisplayName).isEqualTo(disabilityStatus.modifyDisplayName)
  }
}
