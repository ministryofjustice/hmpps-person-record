package uk.gov.justice.digital.hmpps.personrecord.api.controller

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PersonExclusionControllerTest : WebTestBase() {

  @Test
  fun `person exists - returns OK`() {
    stubDeletePersonMatch()

    val prisonNumberOne = randomPrisonNumber()
    val prisonNumberTwo = randomPrisonNumber()
    createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonNumberOne))
      .addPerson(createRandomPrisonPersonDetails(prisonNumberTwo))

    webTestClient
      .post()
      .uri("/admin/exclusion/prisoner")
      .bodyValue(PersonExclusionController.ExclusionRequest(prisonNumberTwo))
      .exchange()
      .expectStatus()
      .isOk
  }

  @Test
  fun `person does not exist - returns NOT_FOUND`() {
    val prisonNumberOne = randomPrisonNumber()
    val prisonNumberTwo = randomPrisonNumber()
    createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonNumberOne))
      .addPerson(createRandomPrisonPersonDetails(prisonNumberTwo))

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

  @Test
  fun `person already in passive state - returns OK`() {
    val prisonNumberOne = randomPrisonNumber()
    createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumberOne)) { markAsPassive() }

    webTestClient
      .post()
      .uri("/admin/exclusion/prisoner")
      .bodyValue(PersonExclusionController.ExclusionRequest(prisonNumberOne))
      .exchange()
      .expectStatus()
      .isOk
  }
}
