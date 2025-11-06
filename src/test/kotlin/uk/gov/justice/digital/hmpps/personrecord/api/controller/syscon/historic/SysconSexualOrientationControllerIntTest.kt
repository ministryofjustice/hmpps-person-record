package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonSexualOrientationRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.RecordType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexualOrientation

class SysconSexualOrientationControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonSexualOrientationRepository: PrisonSexualOrientationRepository

  @Test
  fun `should store sexual orientations against a prison number`() {
    val prisonNumber = randomPrisonNumber()
    val currentCode = randomPrisonSexualOrientation()
    val currentSexualOrientation = PrisonSexualOrientation(
      sexualOrientationCode = currentCode.key,
      startDate = randomDate(),
      endDate = randomDate(),
      current = true,
    )
    val historicCode = randomPrisonSexualOrientation()
    val historicSexualOrientation = PrisonSexualOrientation(
      sexualOrientationCode = historicCode.key,
      startDate = randomDate(),
      endDate = randomDate(),
      current = false,
    )
    val sexualOrientations = listOf(currentSexualOrientation, historicSexualOrientation)

    webTestClient
      .put()
      .uri("/syscon-sync/sexual-orientation/$prisonNumber")
      .bodyValue(sexualOrientations)
      .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk

    val storedSexualOrientations = awaitNotNull { prisonSexualOrientationRepository.findAllByPrisonNumber(prisonNumber) }

    assertThat(storedSexualOrientations).hasSize(2)

    val current = storedSexualOrientations.find { it.recordType == RecordType.CURRENT }
    assertThat(current?.sexualOrientationCode).isEqualTo(currentCode.value)
    assertThat(current?.prisonNumber).isEqualTo(prisonNumber)
    assertThat(current?.startDate).isEqualTo(currentSexualOrientation.startDate)
    assertThat(current?.endDate).isEqualTo(currentSexualOrientation.endDate)

    val historic = storedSexualOrientations.find { it.recordType == RecordType.HISTORIC }
    assertThat(historic?.sexualOrientationCode).isEqualTo(historicCode.value)
    assertThat(historic?.prisonNumber).isEqualTo(prisonNumber)
    assertThat(historic?.startDate).isEqualTo(historicSexualOrientation.startDate)
    assertThat(historic?.endDate).isEqualTo(historicSexualOrientation.endDate)
  }

  @Test
  fun `should update sexual orientations`() {
    val prisonNumber = randomPrisonNumber()
    val sexualOrientations = listOf(
      PrisonSexualOrientation(
      sexualOrientationCode = randomPrisonSexualOrientation().key,
      startDate = randomDate(),
      endDate = randomDate(),
      current = true,
    ),
      PrisonSexualOrientation(
        sexualOrientationCode = randomPrisonSexualOrientation().key,
        startDate = randomDate(),
        endDate = randomDate(),
        current = false,
    ))

    webTestClient
      .put()
      .uri("/syscon-sync/sexual-orientation/$prisonNumber")
      .bodyValue(sexualOrientations)
      .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk

    val createdSexualOrientations = awaitNotNull { prisonSexualOrientationRepository.findAllByPrisonNumber(prisonNumber) }
    assertThat(createdSexualOrientations).hasSize(2)

    val updatedSexualOrientationCode = randomPrisonSexualOrientation()
    val toUpdatedSexualOrientations = listOf(
      PrisonSexualOrientation(
        sexualOrientationCode = updatedSexualOrientationCode.key,
        startDate = randomDate(),
        endDate = randomDate(),
        current = true,
      ),
    )

    webTestClient
      .put()
      .uri("/syscon-sync/sexual-orientation/$prisonNumber")
      .bodyValue(toUpdatedSexualOrientations)
      .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk

    val updatedSexualOrientations = awaitNotNull { prisonSexualOrientationRepository.findAllByPrisonNumber(prisonNumber) }
    assertThat(updatedSexualOrientations).hasSize(1)

    val current = updatedSexualOrientations.find { it.recordType == RecordType.CURRENT }
    assertThat(current?.sexualOrientationCode).isEqualTo(updatedSexualOrientationCode.value)
    assertThat(current?.prisonNumber).isEqualTo(prisonNumber)
    assertThat(current?.startDate).isEqualTo(toUpdatedSexualOrientations.first().startDate)
    assertThat(current?.endDate).isEqualTo(toUpdatedSexualOrientations.first().endDate)
  }

  @Test
  fun `should return Access Denied 403 when role is wrong`() {
    val prisonNumber = randomPrisonNumber()
    val expectedErrorMessage = "Forbidden: Access Denied"
    webTestClient.put()
      .uri("/syscon-sync/sexual-orientation/$prisonNumber")
      .bodyValue(emptyList<PrisonSexualOrientation>())
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
    val prisonNumber = randomPrisonNumber()
    webTestClient.put()
      .uri("/syscon-sync/sexual-orientation/$prisonNumber")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }
}
