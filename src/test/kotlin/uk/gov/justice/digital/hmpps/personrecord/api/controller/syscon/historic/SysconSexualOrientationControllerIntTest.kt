package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientationResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonSexualOrientationRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexualOrientation

class SysconSexualOrientationControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonSexualOrientationRepository: PrisonSexualOrientationRepository

  @Nested
  inner class Creation {

    @Test
    fun `should store current orientations against a prison number`() {
      val prisonNumber = randomPrisonNumber()
      val currentCode = randomPrisonSexualOrientation()
      val currentSexualOrientation = createRandomPrisonSexualOrientation(prisonNumber, currentCode, current = true)

      val historicCode = randomPrisonSexualOrientation()
      val historicSexualOrientation = createRandomPrisonSexualOrientation(prisonNumber, historicCode, current = false)

      val currentCreationResponse = webTestClient
        .put()
        .uri("/syscon-sync/sexual-orientation")
        .bodyValue(currentSexualOrientation)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonSexualOrientationResponse::class.java)
        .returnResult()
        .responseBody!!

      val current = awaitNotNull { prisonSexualOrientationRepository.findByCprSexualOrientationId(currentCreationResponse.cprSexualOrientationId) }

      assertThat(current.sexualOrientationCode).isEqualTo(currentCode.value)
      assertThat(current.prisonNumber).isEqualTo(prisonNumber)
      assertThat(current.startDate).isEqualTo(currentSexualOrientation.startDate)
      assertThat(current.endDate).isEqualTo(currentSexualOrientation.endDate)
      assertThat(current.createDateTime).isEqualTo(currentSexualOrientation.createDateTime)
      assertThat(current.createDisplayName).isEqualTo(currentSexualOrientation.createDisplayName)
      assertThat(current.modifyDateTime).isEqualTo(currentSexualOrientation.modifyDateTime)
      assertThat(current.modifyUserId).isEqualTo(currentSexualOrientation.modifyUserId)
      assertThat(current.modifyDisplayName).isEqualTo(currentSexualOrientation.modifyDisplayName)

      val historicCreationResponse = webTestClient
        .put()
        .uri("/syscon-sync/sexual-orientation")
        .bodyValue(historicSexualOrientation)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonSexualOrientationResponse::class.java)
        .returnResult()
        .responseBody!!

      val historic = awaitNotNull { prisonSexualOrientationRepository.findByCprSexualOrientationId(historicCreationResponse.cprSexualOrientationId) }

      assertThat(historic.sexualOrientationCode).isEqualTo(historicCode.value)
      assertThat(historic.prisonNumber).isEqualTo(prisonNumber)
      assertThat(historic.startDate).isEqualTo(historicSexualOrientation.startDate)
      assertThat(historic.endDate).isEqualTo(historicSexualOrientation.endDate)
    }
  }

  @Nested
  inner class Update {

//    @Test
//    fun `should update sexual orientations`() {
//      val prisonNumber = randomPrisonNumber()
//      val sexualOrientations = listOf(
//        PrisonSexualOrientation(
//          sexualOrientationCode = randomPrisonSexualOrientation().key,
//          startDate = randomDate(),
//          endDate = randomDate(),
//          current = true,
//        ),
//        PrisonSexualOrientation(
//          sexualOrientationCode = randomPrisonSexualOrientation().key,
//          startDate = randomDate(),
//          endDate = randomDate(),
//          current = false,
//        ),
//      )
//
//      webTestClient
//        .put()
//        .uri("/syscon-sync/sexual-orientation/$prisonNumber")
//        .bodyValue(sexualOrientations)
//        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
//        .exchange()
//        .expectStatus()
//        .isOk
//
//      val createdSexualOrientations = awaitNotNull { prisonSexualOrientationRepository.findAllByPrisonNumber(prisonNumber) }
//      assertThat(createdSexualOrientations).hasSize(2)
//
//      val updatedSexualOrientationCode = randomPrisonSexualOrientation()
//      val toUpdatedSexualOrientations = listOf(
//        PrisonSexualOrientation(
//          sexualOrientationCode = updatedSexualOrientationCode.key,
//          startDate = randomDate(),
//          endDate = randomDate(),
//          current = true,
//        ),
//      )
//
//      webTestClient
//        .put()
//        .uri("/syscon-sync/sexual-orientation/$prisonNumber")
//        .bodyValue(toUpdatedSexualOrientations)
//        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
//        .exchange()
//        .expectStatus()
//        .isOk
//
//      val updatedSexualOrientations = awaitNotNull { prisonSexualOrientationRepository.findAllByPrisonNumber(prisonNumber) }
//      assertThat(updatedSexualOrientations).hasSize(1)
//
//      val current = updatedSexualOrientations.find { it.recordType == RecordType.CURRENT }
//      assertThat(current?.sexualOrientationCode).isEqualTo(updatedSexualOrientationCode.value)
//      assertThat(current?.prisonNumber).isEqualTo(prisonNumber)
//      assertThat(current?.startDate).isEqualTo(toUpdatedSexualOrientations.first().startDate)
//      assertThat(current?.endDate).isEqualTo(toUpdatedSexualOrientations.first().endDate)
//    }
  }

  @Test
  fun `should return Access Denied 403 when role is wrong`() {
    val expectedErrorMessage = "Forbidden: Access Denied"
    webTestClient.put()
      .uri("/syscon-sync/sexual-orientation")
      .bodyValue(createRandomPrisonSexualOrientation(randomPrisonNumber(), randomPrisonSexualOrientation(), true))
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

  private fun createRandomPrisonSexualOrientation(prisonNumber: String, code: Map.Entry<String, SexualOrientation>, current: Boolean): PrisonSexualOrientation = PrisonSexualOrientation(
    prisonNumber = prisonNumber,
    sexualOrientationCode = code.key,
    startDate = randomDate(),
    endDate = randomDate(),
    createDateTime = randomDateTime(),
    createDisplayName = randomName(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    modifyDisplayName = randomName(),
    current = current,
  )
}
