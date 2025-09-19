package uk.gov.justice.digital.hmpps.personrecord.api.controller

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class QueueAdminIntTest : WebTestBase() {

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
  fun `should return FORBIDDEN for request to purge dead letter queue with wrong role`() {
    webTestClient
      .put()
      .uri("/queue-admin/purge-queue/cpr_court_cases_queue_dlq.fifo")
      .authorised(listOf("WRONG_ROLE"))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should return UNAUTHORISED for unauthorised request to purge dead letter queue`() {
    webTestClient
      .put()
      .uri("/queue-admin/purge-queue/cpr_court_cases_queue_dlq.fifo")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return 200 for authorised request to purge dead letter queue`() {
    webTestClient
      .put()
      .uri("/queue-admin/purge-queue/cpr_court_cases_queue_dlq.fifo")
      .authorised()
      .exchange()
      .expectStatus()
      .isOk
  }
}
