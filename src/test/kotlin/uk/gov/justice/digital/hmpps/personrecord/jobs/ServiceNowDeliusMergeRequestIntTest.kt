package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class ServiceNowDeliusMergeRequestIntTest : WebTestBase() {

  @Test
  fun `sends merge request to ServiceNow`() {
    webTestClient.post()
      .uri("/jobs/service-now/generate-delius-merge-requests")
      .exchange()
      .expectStatus()
      .isOk
  }
}
