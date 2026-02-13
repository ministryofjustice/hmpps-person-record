package uk.gov.justice.digital.hmpps.personrecord.api.controller

import org.junit.jupiter.api.Test
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
      .uri("/admin/exclusion/prisoner")
      .bodyValue(PersonExclusionController.ExclusionRequest(prisonerNumberTwo))
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

    webTestClient
      .post()
      .uri("/admin/exclusion/prisoner")
      .bodyValue(PersonExclusionController.ExclusionRequest(randomPrisonNumber()))
      .exchange()
      .expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("userMessage")
      .isEqualTo("Not found: Person not found")
  }
}