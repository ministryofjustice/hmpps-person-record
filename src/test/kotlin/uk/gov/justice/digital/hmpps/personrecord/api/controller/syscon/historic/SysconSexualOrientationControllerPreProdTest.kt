package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexualOrientation

@ActiveProfiles("preprod")
class SysconSexualOrientationControllerPreProdTest : WebTestBase() {

  @Test
  fun `should update person sexual orientation`() {
    val prisonNumber = randomPrisonNumber()
    createPerson(createRandomPrisonPersonDetails(prisonNumber))

    val originalEntity = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
    assertThat(originalEntity.sexualOrientation).isNull()

    val sexualOrientationCode = randomPrisonSexualOrientation()
    val prisonSexualOrientation = createRandomPrisonSexualOrientation(sexualOrientationCode.key)
    postSexualOrientation(prisonNumber, prisonSexualOrientation)

    assertCorrectValuesSaved(prisonNumber, originalEntity, sexualOrientationCode.value)
  }

  private fun postSexualOrientation(
    prisonNumber: String,
    sexualOrientation: PrisonSexualOrientation,
  ) {
    webTestClient
      .post()
      .uri(sexualOrientationUrl(prisonNumber))
      .bodyValue(sexualOrientation)
      .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun assertCorrectValuesSaved(
    prisonNumber: String,
    originalEntity: PersonEntity,
    sexualOrientation: SexualOrientation?,
  ) {
    awaitAssert {
      val updatedEntity = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(updatedEntity.sexualOrientation).isEqualTo(sexualOrientation)
      assertThat(updatedEntity.getPrimaryName().updateId).isNotEqualTo(originalEntity.getPrimaryName().updateId)
      assertThat(updatedEntity.getPrimaryName().dateOfBirth).isEqualTo(originalEntity.getPrimaryName().dateOfBirth)
      assertThat(updatedEntity.getPrimaryName().firstName).isEqualTo(originalEntity.getPrimaryName().firstName)
      assertThat(updatedEntity.getPrimaryName().lastName).isEqualTo(originalEntity.getPrimaryName().lastName)
    }
  }

  private fun createRandomPrisonSexualOrientation(code: String?): PrisonSexualOrientation = PrisonSexualOrientation(
    sexualOrientationCode = code,
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
  )

  private fun sexualOrientationUrl(prisonNumber: String) = "/syscon-sync/sexual-orientation/$prisonNumber"
}
