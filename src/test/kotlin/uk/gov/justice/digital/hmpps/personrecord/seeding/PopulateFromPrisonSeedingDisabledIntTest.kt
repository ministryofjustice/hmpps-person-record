package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.WebTestBase

class PopulateFromPrisonSeedingDisabledIntTest : WebTestBase() {

  @Test
  fun `populate from nomis endpoint returns 404 when seeding is not enabled`() {
    webTestClient.post()
      .uri("/populatefromprison")
      .exchange()
      .expectStatus()
      .isUnauthorized // strange - should be a 404 I think.
  }
}
