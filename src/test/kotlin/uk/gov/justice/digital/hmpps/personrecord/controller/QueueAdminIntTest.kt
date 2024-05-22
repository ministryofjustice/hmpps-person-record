package uk.gov.justice.digital.hmpps.personrecord.controller

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.WebTestBase

class QueueAdminIntTest() : WebTestBase() {

  @Test
  fun `should return HTTP Location header containing the URL of new person`() {
    webTestClient
      .put()
      .uri("/queue-admin/retry-all-dlqs")
      .exchange()
      .expectStatus()
      .isOk
  }
}
