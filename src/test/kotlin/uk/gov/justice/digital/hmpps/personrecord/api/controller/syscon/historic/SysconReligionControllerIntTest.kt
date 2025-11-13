package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.*
import java.util.*

class SysconReligionControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class Creation {

    @Test
    fun `should store current religion against a prison number`() {
      val prisonNumber = randomPrisonNumber()
      val currentReligion = createRandomReligion(prisonNumber, current = true)

      val historicReligion = createRandomReligion(prisonNumber, current = false)

      val currentCreationResponse = webTestClient
        .post()
        .uri("/syscon-sync/religion")
        .bodyValue(currentReligion)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonReligionResponse::class.java)
        .returnResult()
        .responseBody!!

      assertCorrectValuesSaved(currentReligion, currentCreationResponse.cprReligionId)

      val historicCreationResponse = webTestClient
        .post()
        .uri("/syscon-sync/religion")
        .bodyValue(historicReligion)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonReligionResponse::class.java)
        .returnResult()
        .responseBody!!

      assertCorrectValuesSaved(historicReligion, historicCreationResponse.cprReligionId)
    }
  }

  @Nested
  inner class Update {

    @Test
    fun `should update a religion`() {
      val prisonNumber = randomPrisonNumber()

      val currentReligion = createRandomReligion(prisonNumber, current = true)

      val currentCreationResponse = webTestClient
        .post()
        .uri("/syscon-sync/religion")
        .bodyValue(currentReligion)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonReligionResponse::class.java)
        .returnResult()
        .responseBody!!

      assertCorrectValuesSaved(currentReligion, currentCreationResponse.cprReligionId)

      val updatedReligion = createRandomReligion(prisonNumber, current = false)

      webTestClient
        .put()
        .uri("/syscon-sync/religion/${currentCreationResponse.cprReligionId}")
        .bodyValue(updatedReligion)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isOk

      assertCorrectValuesSaved(updatedReligion, currentCreationResponse.cprReligionId)
    }
  }

  @Test
  fun `should return Access Denied 403 when role is wrong`() {
    val expectedErrorMessage = "Forbidden: Access Denied"
    webTestClient.post()
      .uri("/syscon-sync/religion")
      .bodyValue(createRandomReligion(randomPrisonNumber(), true))
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
      .uri("/syscon-sync/religion")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  private fun createRandomReligion(prisonNumber: String, current: Boolean) = PrisonReligion(
    prisonNumber = prisonNumber,
    religionStatus = randomName(),
    changeReasonKnown = randomName(),
    comments = randomName(),
    verified = randomBoolean(),
    religionCode = randomName(),
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

  private fun assertCorrectValuesSaved(
    religion: PrisonReligion,
    statusId: UUID,
  ) {
    val religionEntity = awaitNotNull { prisonReligionRepository.findByCprReligionId(statusId) }

    assertThat(religionEntity.verified).isEqualTo(religion.verified)
    assertThat(religionEntity.prisonNumber).isEqualTo(religion.prisonNumber)
    assertThat(religionEntity.comments).isEqualTo(religion.comments)
    assertThat(religionEntity.changeReasonKnown).isEqualTo(religion.changeReasonKnown)
    assertThat(religionEntity.status).isEqualTo(religion.religionStatus)
    assertThat(religionEntity.code).isEqualTo(religion.religionCode)
    assertThat(religionEntity.startDate).isEqualTo(religion.startDate)
    assertThat(religionEntity.endDate).isEqualTo(religion.endDate)
    assertThat(religionEntity.createUserId).isEqualTo(religion.createUserId)
    assertThat(religionEntity.createDateTime).isEqualTo(religion.createDateTime)
    assertThat(religionEntity.createDisplayName).isEqualTo(religion.createDisplayName)
    assertThat(religionEntity.modifyDateTime).isEqualTo(religion.modifyDateTime)
    assertThat(religionEntity.modifyUserId).isEqualTo(religion.modifyUserId)
    assertThat(religionEntity.modifyDisplayName).isEqualTo(religion.modifyDisplayName)
    assertThat(religionEntity.prisonRecordType).isEqualTo(PrisonRecordType.from(religion.current))
  }
}
