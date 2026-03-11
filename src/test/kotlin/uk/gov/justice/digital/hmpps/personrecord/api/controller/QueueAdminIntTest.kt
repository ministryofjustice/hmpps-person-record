package uk.gov.justice.digital.hmpps.personrecord.api.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.QUEUE_ADMIN
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class QueueAdminIntTest {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  internal lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  fun WebTestClient.RequestHeadersSpec<*>.authorised(roles: List<String> = listOf(QUEUE_ADMIN)): WebTestClient.RequestBodySpec = headers(jwtAuthorisationHelper.setAuthorisationHeader(roles = roles)) as WebTestClient.RequestBodySpec

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
