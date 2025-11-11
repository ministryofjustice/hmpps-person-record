package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonNationalityResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonNationalityRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconNationalityControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonNationalityRepository: PrisonNationalityRepository

  @Nested
  inner class Creation {

    @Test
    fun `should store current nationality against a prison number`() {
      val prisonNumber = randomPrisonNumber()
      val currentCode = randomPrisonNationalityCode()
      val currentNationality = createRandomPrisonNationality(prisonNumber, currentCode, current = true)

      val historicCode = randomPrisonNationalityCode()
      val historicNationality = createRandomPrisonNationality(prisonNumber, historicCode, current = false)

      val currentCreationResponse = webTestClient
        .post()
        .uri("/syscon-sync/nationality")
        .bodyValue(currentNationality)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonNationalityResponse::class.java)
        .returnResult()
        .responseBody!!

      val current = awaitNotNull { prisonNationalityRepository.findByCprNationalityId(currentCreationResponse.cprNationalityId) }

      assertThat(current.nationalityCode).isEqualTo(NationalityCode.fromPrisonMapping(currentCode))
      assertThat(current.prisonNumber).isEqualTo(prisonNumber)
      assertThat(current.startDate).isEqualTo(currentNationality.startDate)
      assertThat(current.endDate).isEqualTo(currentNationality.endDate)
      assertThat(current.createUserId).isEqualTo(currentNationality.createUserId)
      assertThat(current.createDateTime).isEqualTo(currentNationality.createDateTime)
      assertThat(current.createDisplayName).isEqualTo(currentNationality.createDisplayName)
      assertThat(current.modifyDateTime).isEqualTo(currentNationality.modifyDateTime)
      assertThat(current.modifyUserId).isEqualTo(currentNationality.modifyUserId)
      assertThat(current.modifyDisplayName).isEqualTo(currentNationality.modifyDisplayName)
      assertThat(current.notes).isEqualTo(currentNationality.notes)

      val historicCreationResponse = webTestClient
        .post()
        .uri("/syscon-sync/nationality")
        .bodyValue(historicNationality)
        .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody(PrisonNationalityResponse::class.java)
        .returnResult()
        .responseBody!!

      val historic = awaitNotNull { prisonNationalityRepository.findByCprNationalityId(historicCreationResponse.cprNationalityId) }

      assertThat(historic.nationalityCode).isEqualTo(NationalityCode.fromPrisonMapping(historicCode))
      assertThat(historic.prisonNumber).isEqualTo(prisonNumber)
      assertThat(historic.startDate).isEqualTo(historicNationality.startDate)
      assertThat(historic.endDate).isEqualTo(historicNationality.endDate)
      assertThat(historic.createUserId).isEqualTo(historicNationality.createUserId)
      assertThat(historic.createDateTime).isEqualTo(historicNationality.createDateTime)
      assertThat(historic.createDisplayName).isEqualTo(historicNationality.createDisplayName)
      assertThat(historic.modifyDateTime).isEqualTo(historicNationality.modifyDateTime)
      assertThat(historic.modifyUserId).isEqualTo(historicNationality.modifyUserId)
      assertThat(historic.modifyDisplayName).isEqualTo(historicNationality.modifyDisplayName)
      assertThat(historic.notes).isEqualTo(historicNationality.notes)
    }
  }

  private fun createRandomPrisonNationality(prisonNumber: String, code: String, current: Boolean): PrisonNationality = PrisonNationality(
    prisonNumber = prisonNumber,
    nationalityCode = code,
    startDate = randomDate(),
    endDate = randomDate(),
    createUserId = randomName(),
    createDateTime = randomDateTime(),
    createDisplayName = randomName(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    modifyDisplayName = randomName(),
    current = current,
    notes = randomName(),
  )
}
