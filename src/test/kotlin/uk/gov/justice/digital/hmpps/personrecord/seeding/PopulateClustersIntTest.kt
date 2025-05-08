package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

@ActiveProfiles("seeding")
class PopulateClustersIntTest : WebTestBase() {
  @Test
  fun `populate clusters`() {
    webTestClient.post()
      .uri("/populateclusters")
      .exchange()
      .expectStatus()
      .isOk
  }
}
