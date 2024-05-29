package uk.gov.justice.digital.hmpps.personrecord.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personrecord.security.JwtAuthHelper
import java.time.Duration

abstract class WebTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  internal fun WebTestClient.RequestHeadersSpec<*>.authorised(roles: List<String> = listOf("ROLE_QUEUE_ADMIN")): WebTestClient.RequestBodySpec {
    val bearerToken = jwtHelper.createJwt(
      subject = "hmpps-person-record",
      expiryTime = Duration.ofMinutes(1L),
      roles = roles,
    )
    return header("authorization", "Bearer $bearerToken") as WebTestClient.RequestBodySpec
  }
}
