package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.QUEUE_ADMIN
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@AutoConfigureWebTestClient
abstract class WebTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  internal lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  protected fun WebTestClient.RequestHeadersSpec<*>.authorised(roles: List<String> = listOf(QUEUE_ADMIN)): WebTestClient.RequestBodySpec = headers(jwtAuthorisationHelper.setAuthorisationHeader(roles = roles)) as WebTestClient.RequestBodySpec

  protected final inline fun <reified T : Any> sendRequestAsserted(
    url: String,
    body: Any,
    roles: List<String>,
    expectedStatus: HttpStatus,
    sendAuthorised: Boolean = true,
  ): WebTestClient.BodySpec<T, *> {
    val requestSpec = webTestClient
      .post()
      .uri(url)
      .bodyValue(body)

    val responseSpec = when (sendAuthorised) {
      true -> requestSpec.authorised(roles).exchange()
      false -> requestSpec.exchange()
    }.expectStatus().isEqualTo(expectedStatus.value())
    return responseSpec.expectBody<T>()
  }
}
