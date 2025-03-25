package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.Duration

abstract class WebTestBase : MessagingMultiNodeTestBase() {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  internal lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  internal fun WebTestClient.RequestHeadersSpec<*>.authorised(roles: List<String> = listOf(Roles.QUEUE_ADMIN)): WebTestClient.RequestBodySpec {
    val bearerToken = jwtAuthorisationHelper.createJwtAccessToken(
      clientId = "hmpps-person-record",
      expiryTime = Duration.ofMinutes(1L),
      roles = roles,
    )
    return header("authorization", "Bearer $bearerToken") as WebTestClient.RequestBodySpec
  }
}
