package uk.gov.justice.digital.hmpps.personrecord.controller

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.WebTestBase

class QueueAdminIntTest() : WebTestBase() {

  @Test
  fun `should retry dead letter queue`() {
    webTestClient
      .put()
      .uri("/queue-admin/retry-all-dlqs")
      .authorised()
      .exchange()
      .expectStatus()
      .isOk
  }

  @Test
  fun `should return 401 for unauthorised request to purge dead letter queue`() {
    webTestClient
      .put()
      .uri("/queue-admin/purge-queue/cpr_court_case_events_queue_dlq")
      .authorised(listOf("WRONG_ROLE"))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return 200 for authorised request to purge dead letter queue`() {
    webTestClient
      .put()
      .uri("/queue-admin/purge-queue/cpr_court_case_events_queue_dlq")
      .authorised()
      .exchange()
      .expectStatus()
      .isOk
  }
}
