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
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconDisabilityStatusControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonDisabilityStatusRepository: PrisonDisabilityStatusRepository

  @Nested
  inner class Creation {

    @Test
    fun `should store current disability status against a prison number`() {
      val prisonNumber = randomPrisonNumber()
//      val disabilityStatus = randomDisabilityStatus()
      val currentDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber)
      val historicDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber)

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
    }
  }

  private fun createRandomPrisonDisabilityStatus(prisonNumber: String): PrisonDisabilityStatus = PrisonDisabilityStatus(
    prisonNumber = prisonNumber,
  )
}
