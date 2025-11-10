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
import uk.gov.justice.digital.hmpps.personrecord.test.randomDisabilityStatus
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconDisabilityStatusControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonDisabilityStatusRepository: PrisonDisabilityStatusRepository

  @Nested
  inner class Creation {

    @Test
    fun `should store current disability status against a prison number`() {
      val prisonNumber = randomPrisonNumber()
      val currentDisability = randomDisabilityStatus()
      val historicDisability = randomDisabilityStatus()
      val currentDisabilityStatus = createRandomPrisonDisabilityStatus(prisonNumber, currentDisability, true)
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
    }
  }

  private fun createRandomPrisonDisabilityStatus(prisonNumber: String, status: Boolean, current: Boolean): PrisonDisabilityStatus = PrisonDisabilityStatus(
    prisonNumber = prisonNumber,
    disability = status,
    current = current,
  )
}
