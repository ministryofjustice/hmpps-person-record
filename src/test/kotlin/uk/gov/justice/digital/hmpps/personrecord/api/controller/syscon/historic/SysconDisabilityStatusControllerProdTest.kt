package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatus
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

@ActiveProfiles("prod")
class SysconDisabilityStatusControllerProdTest : WebTestBase() {

  @Test
  fun `should update person disability status`() {
    val prisonNumber = randomPrisonNumber()
    createPerson(createRandomPrisonPersonDetails(prisonNumber))

    val originalEntity = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
    assertThat(originalEntity.disability).isNull()

    val disabilityStatus = createPrisonDisabilityStatus(true)
    postDisabilityStatus(prisonNumber, disabilityStatus)

    awaitAssert {
      val updatedEntity = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(updatedEntity.disability).isEqualTo(disabilityStatus.disability)
      assertThat(updatedEntity.getPrimaryName().updateId).isNotEqualTo(originalEntity.getPrimaryName().updateId)
      assertThat(updatedEntity.getPrimaryName().dateOfBirth).isEqualTo(originalEntity.getPrimaryName().dateOfBirth)
      assertThat(updatedEntity.getPrimaryName().firstName).isEqualTo(originalEntity.getPrimaryName().firstName)
      assertThat(updatedEntity.getPrimaryName().lastName).isEqualTo(originalEntity.getPrimaryName().lastName)
    }
  }

  private fun postDisabilityStatus(prisonNumber: String, disabilityStatus: PrisonDisabilityStatus) {
    webTestClient
      .post()
      .uri(disabilityUrl(prisonNumber))
      .bodyValue(disabilityStatus)
      .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun createPrisonDisabilityStatus(status: Boolean): PrisonDisabilityStatus = PrisonDisabilityStatus(
    disability = status,
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
  )

  private fun disabilityUrl(prisonNumber: String) = "/syscon-sync/disability-status/$prisonNumber"
}
