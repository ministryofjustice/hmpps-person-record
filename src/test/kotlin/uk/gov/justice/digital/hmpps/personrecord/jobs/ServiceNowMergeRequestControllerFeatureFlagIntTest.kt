package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class ServiceNowMergeRequestControllerFeatureFlagIntTest : WebTestBase() {

  @Nested
  @ActiveProfiles("prod")
  inner class Prod {
    @Test
    fun `not available in prod`() {
      webTestClient.post()
        .uri("/jobs/service-now/generate-delius-merge-requests")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }
}
