package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatusResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonDisabilityStatusRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.CURRENT
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.HISTORIC
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomDisability
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconDisabilityStatusControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonDisabilityStatusRepository: PrisonDisabilityStatusRepository

  @Nested
  inner class Creation {

    @Test
    fun `should store current disability status against a prison number`() {
      val prisonNumber = randomPrisonNumber()

      val currentDisability = randomDisability()
      val currentDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, currentDisability, true)

      val historicDisability = randomDisability()
      val historicDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, historicDisability, false)

      val currentCreationResponse = webTestClient
        .post()
        .uri("/syscon-sync/disability-status")
        .bodyValue(currentDisabilityStatus)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonDisabilityStatusResponse::class.java)
        .returnResult()
        .responseBody!!

      val current = awaitNotNull { prisonDisabilityStatusRepository.findByCprDisabilityStatusId(currentCreationResponse.cprDisabilityStatusId) }

      assertThat(current.prisonNumber).isEqualTo(prisonNumber)
      assertThat(current.disability).isEqualTo(currentDisability)
      assertThat(current.prisonRecordType).isEqualTo(CURRENT)
      assertThat(current.startDate).isEqualTo(currentDisabilityStatus.startDate)
      assertThat(current.endDate).isEqualTo(currentDisabilityStatus.endDate)
      assertThat(current.createUserId).isEqualTo(currentDisabilityStatus.createUserId)
      assertThat(current.createDateTime).isEqualTo(currentDisabilityStatus.createDateTime)
      assertThat(current.createDisplayName).isEqualTo(currentDisabilityStatus.createDisplayName)
      assertThat(current.modifyDateTime).isEqualTo(currentDisabilityStatus.modifyDateTime)
      assertThat(current.modifyUserId).isEqualTo(currentDisabilityStatus.modifyUserId)
      assertThat(current.modifyDisplayName).isEqualTo(currentDisabilityStatus.modifyDisplayName)

      val historicCreationResponse = webTestClient
        .post()
        .uri("/syscon-sync/disability-status")
        .bodyValue(historicDisabilityStatus)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonDisabilityStatusResponse::class.java)
        .returnResult()
        .responseBody!!

      val historic = awaitNotNull { prisonDisabilityStatusRepository.findByCprDisabilityStatusId(historicCreationResponse.cprDisabilityStatusId) }

      assertThat(historic.prisonNumber).isEqualTo(prisonNumber)
      assertThat(historic.disability).isEqualTo(historicDisability)
      assertThat(historic.prisonRecordType).isEqualTo(HISTORIC)
      assertThat(historic.startDate).isEqualTo(historicDisabilityStatus.startDate)
      assertThat(historic.endDate).isEqualTo(historicDisabilityStatus.endDate)
      assertThat(historic.createUserId).isEqualTo(historicDisabilityStatus.createUserId)
      assertThat(historic.createDateTime).isEqualTo(historicDisabilityStatus.createDateTime)
      assertThat(historic.createDisplayName).isEqualTo(historicDisabilityStatus.createDisplayName)
      assertThat(historic.modifyDateTime).isEqualTo(historicDisabilityStatus.modifyDateTime)
      assertThat(historic.modifyUserId).isEqualTo(historicDisabilityStatus.modifyUserId)
      assertThat(historic.modifyDisplayName).isEqualTo(historicDisabilityStatus.modifyDisplayName)
    }
  }

  @Nested
  inner class Update {
    @Test
    fun `should update a disability status`() {
      val prisonNumber = randomPrisonNumber()

      val currentDisability = randomDisability()
      val currentDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, currentDisability, true)

      val currentCreationResponse = webTestClient
        .post()
        .uri("/syscon-sync/disability-status")
        .bodyValue(currentDisabilityStatus)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonDisabilityStatusResponse::class.java)
        .returnResult()
        .responseBody!!

      val current = awaitNotNull { prisonDisabilityStatusRepository.findByCprDisabilityStatusId(currentCreationResponse.cprDisabilityStatusId) }

      assertThat(current.prisonNumber).isEqualTo(prisonNumber)

      val updateedDisability = randomDisability()
      val updatedDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, updateedDisability, false)

      webTestClient
        .put()
        .uri("/syscon-sync/disability-status/${current.cprDisabilityStatusId}")
        .bodyValue(updatedDisabilityStatus)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isOk

      val updated = awaitNotNull { prisonDisabilityStatusRepository.findByCprDisabilityStatusId(currentCreationResponse.cprDisabilityStatusId) }

      assertThat(updated.prisonNumber).isEqualTo(prisonNumber)
      assertThat(updated.disability).isEqualTo(updateedDisability)
      assertThat(updated.prisonRecordType).isEqualTo(HISTORIC)
      assertThat(updated.startDate).isEqualTo(updatedDisabilityStatus.startDate)
      assertThat(updated.endDate).isEqualTo(updatedDisabilityStatus.endDate)
      assertThat(updated.createUserId).isEqualTo(updatedDisabilityStatus.createUserId)
      assertThat(updated.createDateTime).isEqualTo(updatedDisabilityStatus.createDateTime)
      assertThat(updated.createDisplayName).isEqualTo(updatedDisabilityStatus.createDisplayName)
      assertThat(updated.modifyDateTime).isEqualTo(updatedDisabilityStatus.modifyDateTime)
      assertThat(updated.modifyUserId).isEqualTo(updatedDisabilityStatus.modifyUserId)
      assertThat(updated.modifyDisplayName).isEqualTo(updatedDisabilityStatus.modifyDisplayName)
    }
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
