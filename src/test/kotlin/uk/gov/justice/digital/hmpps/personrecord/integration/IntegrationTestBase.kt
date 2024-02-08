package uk.gov.justice.digital.hmpps.personrecord.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.security.JwtAuthHelper
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Duration

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

  @Autowired
  lateinit var mockMvc: MockMvc

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var personRepository: PersonRepository

  @SpyBean
  lateinit var telemetryService: TelemetryService

  val courtCaseEventsTopic by lazy {
    hmppsQueueService.findByTopicId("courtcaseeventstopic")
  }
  val cprCourtCaseEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprcourtcaseeventsqueue")
  }
  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
  }
  val cprDeliusOffenderEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprdeliusoffendereventsqueue")
  }

  @RegisterExtension
  var wireMockExtension = WireMockExtension.newInstance()
    .options(wireMockConfig().port(8090))
    .build()

  @BeforeEach
  fun beforeEach() {
    cprCourtCaseEventsQueue?.sqsDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(cprCourtCaseEventsQueue?.dlqUrl).build()).get()
    cprCourtCaseEventsQueue?.sqsClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(cprCourtCaseEventsQueue?.queueUrl).build()).get()
    cprDeliusOffenderEventsQueue?.sqsClient?.purgeQueue(PurgeQueueRequest.builder().queueUrl(cprDeliusOffenderEventsQueue?.queueUrl).build())
  }
  companion object {

    /*
     @JvmStatic used instead of @Container annotation to prevent the premature closing of
     the DB container after execution of first test
     */
    @JvmStatic
    val postgresSQLContainer = PostgreSQLContainer("postgres:latest")

    @JvmStatic
    val localStackContainer = LocalStackHelper.instance

    @BeforeAll
    @JvmStatic
    fun beforeAll() {
      postgresSQLContainer.start()
    }

    @DynamicPropertySource
    @JvmStatic
    fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
      registry.add("spring.datasource.url", postgresSQLContainer::getJdbcUrl)
      registry.add("spring.datasource.username", postgresSQLContainer::getUsername)
      registry.add("spring.datasource.password", postgresSQLContainer::getPassword)
      registry.add("hmpps.sqs.localstackUrl") { localStackContainer?.getEndpointOverride(LocalStackContainer.Service.SNS) }
      registry.add("hmpps.sqs.region") { localStackContainer?.region }
    }

    const val VIEW_PERSON_DATA_ROLE = "ROLE_VIEW_PERSON_DATA"
  }

  internal fun setAuthorisation(user: String = "hmpps-person-record", roles: List<String> = listOf()): HttpHeaders {
    val token = jwtHelper.createJwt(
      subject = user,
      expiryTime = Duration.ofHours(1L),
      roles = roles,
    )
    val httpHeaders = HttpHeaders()
    httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer $token")
    return httpHeaders
  }
}
