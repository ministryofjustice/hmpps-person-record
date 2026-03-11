package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.QUEUE_ADMIN
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@AutoConfigureWebTestClient
@TestPropertySource(properties = ["spring.autoconfigure.exclude=uk.gov.justice.hmpps.sqs.HmppsSqsConfiguration"])
abstract class WebTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  internal lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  @MockitoBean
  private lateinit var hmppsQueueService: HmppsQueueService

  protected fun WebTestClient.RequestHeadersSpec<*>.authorised(roles: List<String> = listOf(QUEUE_ADMIN)): WebTestClient.RequestBodySpec = headers(jwtAuthorisationHelper.setAuthorisationHeader(roles = roles)) as WebTestClient.RequestBodySpec

  protected final inline fun <reified T : Any> sendPostRequestAsserted(
    url: String,
    body: Any,
    roles: List<String>,
    expectedStatus: HttpStatus,
    sendAuthorised: Boolean = true,
  ): WebTestClient.BodySpec<T, *> = sendRequestAsserted(url, body, roles, expectedStatus, sendAuthorised, HttpMethod.POST)

  protected final inline fun <reified T : Any> sendPutRequestAsserted(
    url: String,
    body: Any,
    roles: List<String>,
    expectedStatus: HttpStatus,
    sendAuthorised: Boolean = true,
  ): WebTestClient.BodySpec<T, *> = sendRequestAsserted(url, body, roles, expectedStatus, sendAuthorised, HttpMethod.PUT)

  protected final inline fun <reified T : Any> sendGetRequestAsserted(
    url: String,
    roles: List<String>,
    expectedStatus: HttpStatus,
    sendAuthorised: Boolean = true,
  ): WebTestClient.BodySpec<T, *> = sendRequestAsserted(url, null, roles, expectedStatus, sendAuthorised, HttpMethod.GET)

  protected final inline fun <reified T : Any> sendRequestAsserted(
    url: String,
    body: Any?,
    roles: List<String>,
    expectedStatus: HttpStatus,
    sendAuthorised: Boolean = true,
    methodType: HttpMethod,
  ): WebTestClient.BodySpec<T, *> {
    val requestSpec = webTestClient
      .method(methodType)
      .uri(url)

    val requestSpecReady = when (methodType) {
      HttpMethod.GET -> requestSpec
      else -> requestSpec.bodyValue(body!!)
    }

    val responseSpec = when (sendAuthorised) {
      true -> requestSpecReady.authorised(roles).exchange()
      false -> requestSpecReady.exchange()
    }.expectStatus().isEqualTo(expectedStatus.value())
    return responseSpec.expectBody<T>()
  }
}
