package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonImmigrationStatus
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

@ActiveProfiles("prod")
class SysconImmigrationStatusControllerProdTest : WebTestBase() {

  @Test
  fun `should overwrite aliases upon an update to person immigration status`() {
    val prisonNumber = randomPrisonNumber()
    createPerson(createRandomPrisonPersonDetails(prisonNumber))

    val originalEntity = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
    assertThat(originalEntity.immigrationStatus).isNull()

    val immigrationStatus = createPrisonImmigrationStatus()
    postImmigrationStatus(prisonNumber, immigrationStatus)

    assertCorrectValuesSaved(prisonNumber, originalEntity, immigrationStatus.interestToImmigration)
  }

  private fun postImmigrationStatus(prisonNumber: String, immigrationStatus: Any) {
    webTestClient
      .post()
      .uri(immigrationUrl(prisonNumber))
      .bodyValue(immigrationStatus)
      .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun assertCorrectValuesSaved(prisonNumber: String, originalEntity: PersonEntity, expectedImmigrationStatus: Boolean?) {
    awaitAssert {
      val updatedEntity = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(updatedEntity.immigrationStatus).isEqualTo(expectedImmigrationStatus)
      assertThat(updatedEntity.getPrimaryName().updateId).isNotEqualTo(originalEntity.getPrimaryName().updateId)
      assertThat(updatedEntity.getPrimaryName().dateOfBirth).isEqualTo(originalEntity.getPrimaryName().dateOfBirth)
      assertThat(updatedEntity.getPrimaryName().firstName).isEqualTo(originalEntity.getPrimaryName().firstName)
      assertThat(updatedEntity.getPrimaryName().lastName).isEqualTo(originalEntity.getPrimaryName().lastName)
    }
  }

  private fun createPrisonImmigrationStatus(): PrisonImmigrationStatus = PrisonImmigrationStatus(
    interestToImmigration = randomBoolean(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
  )

  private fun immigrationUrl(prisonNumber: String) = "/syscon-sync/immigration-status/$prisonNumber"
}
