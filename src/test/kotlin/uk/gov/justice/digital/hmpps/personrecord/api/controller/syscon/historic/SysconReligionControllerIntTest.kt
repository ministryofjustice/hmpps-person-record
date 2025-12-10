package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconReligionControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class Creation {

    @Test
    fun `should save current religion against a prison number`() {
      val prisonNumber = randomPrisonNumber()
      val currentReligion = createRandomReligion(randomName(), current = true)

      postReligion(prisonNumber, currentReligion)
      assertCorrectValuesSaved(prisonNumber, currentReligion)

      val historicReligion = createRandomReligion(randomName(), current = false)

      postReligion(prisonNumber, historicReligion)
      assertCorrectValuesSaved(prisonNumber, historicReligion)
    }

    @Test
    fun `should save current religion against a prison number when code is null`() {
      val prisonNumber = randomPrisonNumber()
      val currentReligion = createRandomReligion(null, current = true)

      postReligion(prisonNumber, currentReligion)
      assertCorrectValuesSaved(prisonNumber, currentReligion)
    }
  }

  @Nested
  inner class Update {

    @Test
    fun `should update an existing religion`() {
      val prisonNumber = randomPrisonNumber()
      val currentReligion = createRandomReligion(randomName(), current = true)

      postReligion(prisonNumber, currentReligion)
      assertCorrectValuesSaved(prisonNumber, currentReligion)

      val updatedReligion = createRandomReligion(randomName(), current = false)

      postReligion(prisonNumber, updatedReligion)
      assertCorrectValuesSaved(prisonNumber, updatedReligion)
    }

    @Test
    fun `should update an existing religion when code is null`() {
      val prisonNumber = randomPrisonNumber()
      val currentReligion = createRandomReligion(randomName(), current = true)

      postReligion(prisonNumber, currentReligion)
      assertCorrectValuesSaved(prisonNumber, currentReligion)

      val updatedReligion = createRandomReligion(null, current = false)

      postReligion(prisonNumber, updatedReligion)
      assertCorrectValuesSaved(prisonNumber, updatedReligion)
    }
  }

  @Nested
  inner class Auth {
    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.post()
        .uri("/syscon-sync/religion/" + randomPrisonNumber())
        .bodyValue(createRandomReligion(randomName(), true))
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
        .uri("/syscon-sync/religion/" + randomPrisonNumber())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun postReligion(prisonNumber: String, religion: PrisonReligion) {
    webTestClient
      .post()
      .uri("/syscon-sync/religion/$prisonNumber")
      .bodyValue(religion)
      .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun createRandomReligion(code: String?, current: Boolean) = PrisonReligion(
    changeReasonKnown = randomBoolean(),
    comments = randomName(),
    verified = randomBoolean(),
    religionCode = code,
    startDate = randomDate(),
    endDate = randomDate(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    current = current,
  )

  private fun assertCorrectValuesSaved(
    prisonNumber: String,
    religion: PrisonReligion,
  ) {
    val religionEntity = awaitNotNull { prisonReligionRepository.findByPrisonNumber(prisonNumber) }

    assertThat(religionEntity.prisonNumber).isEqualTo(prisonNumber)
    assertThat(religionEntity.verified).isEqualTo(religion.verified)
    assertThat(religionEntity.comments).isEqualTo(religion.comments)
    assertThat(religionEntity.changeReasonKnown).isEqualTo(religion.changeReasonKnown)
    assertThat(religionEntity.code).isEqualTo(religion.religionCode)
    assertThat(religionEntity.startDate).isEqualTo(religion.startDate)
    assertThat(religionEntity.endDate).isEqualTo(religion.endDate)
    assertThat(religionEntity.modifyDateTime).isEqualTo(religion.modifyDateTime)
    assertThat(religionEntity.modifyUserId).isEqualTo(religion.modifyUserId)
    assertThat(religionEntity.prisonRecordType).isEqualTo(PrisonRecordType.from(religion.current))
  }
}
