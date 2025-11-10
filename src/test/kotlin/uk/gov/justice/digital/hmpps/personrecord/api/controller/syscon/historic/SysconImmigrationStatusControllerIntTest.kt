package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonImmigrationStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonImmigrationStatusResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonImmigrationStatusRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.*

class SysconImmigrationStatusControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonImmigrationStatusRepository: PrisonImmigrationStatusRepository

  @Nested
  inner class Creation {

    @Test
    fun `should store current immigration status against a prison number`() {
      val prisonNumber = randomPrisonNumber()
      val currentImmigrationStatus = createRandomPrisonImmigrationStatus(prisonNumber, current = true)

      val historicImmigrationStatus = createRandomPrisonImmigrationStatus(prisonNumber, current = false)

      val currentCreationResponse = webTestClient
        .post()
        .uri("/syscon-sync/immigration-status")
        .bodyValue(currentImmigrationStatus)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonImmigrationStatusResponse::class.java)
        .returnResult()
        .responseBody!!

      val current = awaitNotNull { prisonImmigrationStatusRepository.findByCprImmigrationStatusId(currentCreationResponse.cprImmigrationStatusId) }

      assertThat(current.interestToImmigration).isEqualTo(currentImmigrationStatus.interestToImmigration)
      assertThat(current.prisonNumber).isEqualTo(prisonNumber)
      assertThat(current.startDate).isEqualTo(currentImmigrationStatus.startDate)
      assertThat(current.endDate).isEqualTo(currentImmigrationStatus.endDate)
      assertThat(current.createUserId).isEqualTo(currentImmigrationStatus.createUserId)
      assertThat(current.createDateTime).isEqualTo(currentImmigrationStatus.createDateTime)
      assertThat(current.createDisplayName).isEqualTo(currentImmigrationStatus.createDisplayName)
      assertThat(current.modifyDateTime).isEqualTo(currentImmigrationStatus.modifyDateTime)
      assertThat(current.modifyUserId).isEqualTo(currentImmigrationStatus.modifyUserId)
      assertThat(current.modifyDisplayName).isEqualTo(currentImmigrationStatus.modifyDisplayName)
      assertThat(current.prisonRecordType).isEqualTo(PrisonRecordType.CURRENT)

      val historicCreationResponse = webTestClient
        .post()
        .uri("/syscon-sync/immigration-status")
        .bodyValue(historicImmigrationStatus)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonImmigrationStatusResponse::class.java)
        .returnResult()
        .responseBody!!

      val historic = awaitNotNull { prisonImmigrationStatusRepository.findByCprImmigrationStatusId(historicCreationResponse.cprImmigrationStatusId) }

      assertThat(historic.interestToImmigration).isEqualTo(historicImmigrationStatus.interestToImmigration)
      assertThat(historic.prisonNumber).isEqualTo(prisonNumber)
      assertThat(historic.startDate).isEqualTo(historicImmigrationStatus.startDate)
      assertThat(historic.endDate).isEqualTo(historicImmigrationStatus.endDate)
      assertThat(historic.createUserId).isEqualTo(historicImmigrationStatus.createUserId)
      assertThat(historic.createDateTime).isEqualTo(historicImmigrationStatus.createDateTime)
      assertThat(historic.createDisplayName).isEqualTo(historicImmigrationStatus.createDisplayName)
      assertThat(historic.modifyDateTime).isEqualTo(historicImmigrationStatus.modifyDateTime)
      assertThat(historic.modifyUserId).isEqualTo(historicImmigrationStatus.modifyUserId)
      assertThat(historic.modifyDisplayName).isEqualTo(historicImmigrationStatus.modifyDisplayName)
      assertThat(historic.prisonRecordType).isEqualTo(PrisonRecordType.HISTORIC)
    }
  }

  @Nested
  inner class Update {

    @Test
    fun `should update a immigration status`() {
      val prisonNumber = randomPrisonNumber()

      val currentSexualOrientation = createRandomPrisonImmigrationStatus(prisonNumber, current = true)

      val currentCreationResponse = webTestClient
        .post()
        .uri("/syscon-sync/immigration-status")
        .bodyValue(currentSexualOrientation)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonImmigrationStatusResponse::class.java)
        .returnResult()
        .responseBody!!

      val current = awaitNotNull { prisonImmigrationStatusRepository.findByCprImmigrationStatusId(currentCreationResponse.cprImmigrationStatusId) }

      assertThat(current.interestToImmigration).isEqualTo(currentSexualOrientation.interestToImmigration)

      val updatedImmigrationStatus = createRandomPrisonImmigrationStatus(prisonNumber, current = false)

      webTestClient
        .put()
        .uri("/syscon-sync/immigration-status/${current.cprImmigrationStatusId}")
        .bodyValue(updatedImmigrationStatus)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isOk

      val updated = awaitNotNull { prisonImmigrationStatusRepository.findByCprImmigrationStatusId(currentCreationResponse.cprImmigrationStatusId) }

      assertThat(updated.interestToImmigration).isEqualTo(updatedImmigrationStatus.interestToImmigration)
      assertThat(updated.prisonNumber).isEqualTo(prisonNumber)
      assertThat(updated.startDate).isEqualTo(updatedImmigrationStatus.startDate)
      assertThat(updated.endDate).isEqualTo(updatedImmigrationStatus.endDate)
      assertThat(updated.createUserId).isEqualTo(updatedImmigrationStatus.createUserId)
      assertThat(updated.createDateTime).isEqualTo(updatedImmigrationStatus.createDateTime)
      assertThat(updated.createDisplayName).isEqualTo(updatedImmigrationStatus.createDisplayName)
      assertThat(updated.modifyDateTime).isEqualTo(updatedImmigrationStatus.modifyDateTime)
      assertThat(updated.modifyUserId).isEqualTo(updatedImmigrationStatus.modifyUserId)
      assertThat(updated.modifyDisplayName).isEqualTo(updatedImmigrationStatus.modifyDisplayName)
      assertThat(updated.prisonRecordType).isEqualTo(PrisonRecordType.HISTORIC)
    }
  }

  private fun createRandomPrisonImmigrationStatus(prisonNumber: String, current: Boolean): PrisonImmigrationStatus = PrisonImmigrationStatus(
    prisonNumber = prisonNumber,
    interestToImmigration = randomInterestToImmigration(),
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

  private fun randomInterestToImmigration() = listOf(true, false).random()
}
