package uk.gov.justice.digital.hmpps.personrecord.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.health.PersonMatchHealthPing
import uk.gov.justice.digital.hmpps.personrecord.health.PersonRecordHealthPing

class QueueAdminIntTest() : WebTestBase() {

  @MockBean
  private lateinit var personMatchHealthPing: PersonMatchHealthPing

  @MockBean
  @Autowired
  private lateinit var personRecordHealthPing: PersonRecordHealthPing

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
      .uri("/queue-admin/purge-queue/cpr_court_case_events_queue_dlq")
      .authorised(listOf("WRONG_ROLE"))
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
      .authorised()
      .exchange()
      .expectStatus()
      .isOk
  }
}
