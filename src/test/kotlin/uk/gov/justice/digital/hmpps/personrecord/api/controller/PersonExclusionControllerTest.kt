package uk.gov.justice.digital.hmpps.personrecord.api.controller

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_ADMIN_WRITE
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PersonExclusionControllerTest : WebTestBase() {

  @Test
  fun `cluster size greater than 1 - unlinks`() {
    stubDeletePersonMatch()

    val prisonerNumberOne = randomPrisonNumber()
    val prisonerNumberTwo = randomPrisonNumber()
    createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberOne))
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberTwo))

    webTestClient
      .post()
      .uri("/admin/exclusion/person/$prisonerNumberTwo")
      .authorised(roles = listOf(PERSON_RECORD_ADMIN_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  @Test
  fun `non existing prisoner id`() {
    val prisonerNumberOne = randomPrisonNumber()
    val prisonerNumberTwo = randomPrisonNumber()
    createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberOne))
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberTwo))

    val nonExistingPrisonerId = randomPrisonNumber()
    webTestClient
      .post()
      .uri("/admin/exclusion/person/$nonExistingPrisonerId")
      .authorised(roles = listOf(PERSON_RECORD_ADMIN_WRITE))
      .exchange()
      .expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("userMessage")
      .isEqualTo("Not found: Person with prisoner $nonExistingPrisonerId not found")
  }
}