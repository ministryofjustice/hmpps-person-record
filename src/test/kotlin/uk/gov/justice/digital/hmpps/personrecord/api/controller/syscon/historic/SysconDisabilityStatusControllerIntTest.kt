package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatusResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonDisabilityStatusEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonDisabilityStatusRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomDisability
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconDisabilityStatusControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonDisabilityStatusRepository: PrisonDisabilityStatusRepository

  @Nested
  inner class Create {

    @Test
    fun `should store current disability status against a prison number`() {
      val prisonNumber = randomPrisonNumber()

      val currentDisability = randomDisability()
      val currentDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, currentDisability, true)
      val currentCreationResponse = postDisabilityStatus(currentDisabilityStatus)
      val current = awaitNotNull { prisonDisabilityStatusRepository.findByCprDisabilityStatusId(currentCreationResponse.cprDisabilityStatusId) }

      assertCorrectValuesSaved(current, currentDisabilityStatus)

      val historicDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, randomDisability(), false)

      val historicCreationResponse = postDisabilityStatus(historicDisabilityStatus)

      val historic = awaitNotNull { prisonDisabilityStatusRepository.findByCprDisabilityStatusId(historicCreationResponse.cprDisabilityStatusId) }

      assertCorrectValuesSaved(historic, historicDisabilityStatus)
    }
  }

  private fun assertCorrectValuesSaved(
    current: PrisonDisabilityStatusEntity,
    currentDisabilityStatus: PrisonDisabilityStatus,
  ) {
    assertThat(current.prisonNumber).isEqualTo(currentDisabilityStatus.prisonNumber)
    assertThat(current.disability).isEqualTo(currentDisabilityStatus.disability)
    assertThat(current.prisonRecordType).isEqualTo(PrisonRecordType.from(currentDisabilityStatus.current))
    assertThat(current.startDate).isEqualTo(currentDisabilityStatus.startDate)
    assertThat(current.endDate).isEqualTo(currentDisabilityStatus.endDate)
    assertThat(current.createUserId).isEqualTo(currentDisabilityStatus.createUserId)
    assertThat(current.createDateTime).isEqualTo(currentDisabilityStatus.createDateTime)
    assertThat(current.createDisplayName).isEqualTo(currentDisabilityStatus.createDisplayName)
    assertThat(current.modifyDateTime).isEqualTo(currentDisabilityStatus.modifyDateTime)
    assertThat(current.modifyUserId).isEqualTo(currentDisabilityStatus.modifyUserId)
    assertThat(current.modifyDisplayName).isEqualTo(currentDisabilityStatus.modifyDisplayName)
  }

  @Nested
  inner class Update {

    @Test
    fun `should update a disability status`() {
      val prisonNumber = randomPrisonNumber()

      val currentDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, randomDisability(), true)

      val currentCreationResponse = postDisabilityStatus(currentDisabilityStatus)
      val current = awaitNotNull { prisonDisabilityStatusRepository.findByCprDisabilityStatusId(currentCreationResponse.cprDisabilityStatusId) }

      assertCorrectValuesSaved(current, currentDisabilityStatus)

      val updatedDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, randomDisability(), false)

      webTestClient
        .put()
        .uri("/syscon-sync/disability-status/${current.cprDisabilityStatusId}")
        .bodyValue(updatedDisabilityStatus)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isOk

      val updated = awaitNotNull { prisonDisabilityStatusRepository.findByCprDisabilityStatusId(currentCreationResponse.cprDisabilityStatusId) }
      assertCorrectValuesSaved(updated, updatedDisabilityStatus)
    }
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
}
