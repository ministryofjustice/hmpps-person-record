package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonNationality
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

@ActiveProfiles("prod")
class SysconNationalityControllerProdTest : WebTestBase() {

  @Test
  fun `should overwrite aliases upon an nationality insert`() {
    val prisonNumber = randomPrisonNumber()
    val currentNationality = createRandomPrisonNationality(NationalityCode.entries.random().toString())
    val originalPerson = createPerson(createRandomPrisonPersonDetails(prisonNumber))

    postNationality(prisonNumber, currentNationality)
    assertCorrectValuesSaved(prisonNumber, currentNationality, originalPerson)
  }

  private fun postNationality(prisonNumber: String, nationality: PrisonNationality) {
    webTestClient
      .post()
      .uri("/syscon-sync/nationality/$prisonNumber")
      .bodyValue(nationality)
      .authorised(roles = listOf(Roles.PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  @Test
  fun `should overwrite aliases upon an nationality update`() {
    val prisonNumber = randomPrisonNumber()
    val currentCode = NationalityCode.entries.random().toString()
    val currentNationality = createRandomPrisonNationality(currentCode)
    val originalPerson = createPerson(createRandomPrisonPersonDetails(prisonNumber))

    postNationality(prisonNumber, currentNationality)
    assertCorrectValuesSaved(prisonNumber, currentNationality, originalPerson)

    val updatedCode = NationalityCode.entries.random().toString()
    val updatedNationality = createRandomPrisonNationality(updatedCode)

    postNationality(prisonNumber, updatedNationality)
    assertCorrectValuesSaved(prisonNumber, updatedNationality, originalPerson)
  }

  private fun assertCorrectValuesSaved(
    prisonNumber: String,
    nationality: PrisonNationality,
    originalPerson: PersonEntity,
  ) {
    val actualPerson = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }

    assertThat(actualPerson.nationalityNotes).isEqualTo(nationality.notes)
    assertThat(actualPerson.nationalities.size).isEqualTo(1)
    val actualNationality = actualPerson.nationalities.first().nationalityCode
    val expectedNationality = nationality.nationalityCode
    assertThat(actualNationality.name).isEqualTo(expectedNationality)
    assertThat(actualPerson.getPrimaryName().updateId).isNotEqualTo(originalPerson.getPrimaryName().updateId)
  }

  private fun createRandomPrisonNationality(code: String?): PrisonNationality = PrisonNationality(
    nationalityCode = code,
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    notes = randomName(),
  )
}
