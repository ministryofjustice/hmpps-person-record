package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class SeedingDisabledIntTest : WebTestBase() {

  @Test
  fun `populate from prison endpoint not accessible when seeding is not enabled`() {
    webTestClient.post()
      .uri("/populatefromprison")
      .exchange()
      .expectStatus()
      .isUnauthorized // strange - should be a 404 I think.
  }

  @Test
  fun `populate from probation endpoint not accessible when seeding is not enabled`() {
    webTestClient.post()
      .uri("/populatefromprobation")
      .exchange()
      .expectStatus()
      .isUnauthorized // strange - should be a 404 I think.
  }

  @Test
  fun `populate from populate person match endpoint not accessible when seeding is not enabled`() {
    webTestClient.post()
      .uri("/populatepersonmatch")
      .exchange()
      .expectStatus()
      .isUnauthorized // strange - should be a 404 I think.
  }
}
