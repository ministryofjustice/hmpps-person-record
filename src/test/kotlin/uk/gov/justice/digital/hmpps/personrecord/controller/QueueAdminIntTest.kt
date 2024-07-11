package uk.gov.justice.digital.hmpps.personrecord.controller

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class QueueAdminIntTest() : WebTestBase() {

  @Test
  fun `should retry dead letter queue`() {
    webTestClient
      .put()
      .uri("/queue-admin/retry-all-dlqs")
      .exchange()
      .expectStatus()
      .isOk
  }

  @Test
  fun `should return FORBIDDEN for request to purge dead letter queue with wrong role`() {
    webTestClient
      .put()
      .uri("/queue-admin/purge-queue/cpr_court_case_events_queue_dlq")
      .headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `should return UNAUTHORISED for unauthorised request to purge dead letter queue`() {
    webTestClient
      .put()
      .uri("/queue-admin/purge-queue/cpr_court_case_events_queue_dlq")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return 200 for authorised request to purge dead letter queue`() {
    webTestClient
      .put()
      .uri("/queue-admin/purge-queue/cpr_court_case_events_queue_dlq")
      .headers(setAuthorisation(roles = listOf("ROLE_QUEUE_ADMIN")))
      .exchange()
      .expectStatus()
      .isOk
  }
}
