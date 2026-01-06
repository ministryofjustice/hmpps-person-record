package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.QUEUE_ADMIN
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@AutoConfigureWebTestClient
abstract class WebTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  internal lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  internal fun WebTestClient.RequestHeadersSpec<*>.authorised(roles: List<String> = listOf(QUEUE_ADMIN)): WebTestClient.RequestBodySpec = headers(jwtAuthorisationHelper.setAuthorisationHeader(roles = roles)) as WebTestClient.RequestBodySpec
}
