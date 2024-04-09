package uk.gov.justice.digital.hmpps.personrecord.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
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
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PrisonerRepository
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.PrisonerCreatedEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.PrisonerUpdatedEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.security.JwtAuthHelper
import uk.gov.justice.digital.hmpps.personrecord.service.PrisonerService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.Duration
import java.util.concurrent.CompletableFuture

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

  @Autowired
  lateinit var defendantRepository: DefendantRepository

  @Autowired
  lateinit var offenderRepository: OffenderRepository

  @Autowired
  lateinit var prisonerRepository: PrisonerRepository

  @SpyBean
  lateinit var telemetryService: TelemetryService // replace with telemtryClient?

  @SpyBean
  lateinit var prisonerCreatedEventProcessor: PrisonerCreatedEventProcessor

  @SpyBean
  lateinit var prisonerUpdatedEventProcessor: PrisonerUpdatedEventProcessor

  @SpyBean
  lateinit var prisonerService: PrisonerService

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

  @BeforeEach
  fun beforeEach() {
    defendantRepository.deleteAll()
    offenderRepository.deleteAll()
    personRepository.deleteAll()
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

    @JvmStatic
    @RegisterExtension
    var wireMockExtension: WireMockExtension = WireMockExtension.newInstance()
      .options(wireMockConfig().port(8090))
      .build()
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

  internal fun WebTestClient.RequestHeadersSpec<*>.authorised(): WebTestClient.RequestBodySpec {
    val bearerToken = jwtHelper.createJwt(
      subject = "hmpps-person-record",
      expiryTime = Duration.ofMinutes(1L),
      roles = listOf(),
    )
    return header("authorization", "Bearer $bearerToken") as WebTestClient.RequestBodySpec
  }

  internal fun publishHMCTSMessage(message: String, messageType: MessageType) {
    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(message)
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(messageType.name).build(),
        ),
      )
      .build()

    courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()

    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }
  }

  private fun assertDomainEventReceiverQueueHasProcessedMessages() {
    await untilCallTo {
      cprDeliusOffenderEventsQueue?.sqsClient?.countMessagesOnQueue(cprDeliusOffenderEventsQueue!!.queueUrl)
        ?.get()
    } matches { it == 0 }
  }

  fun publishOffenderDomainEvent(eventType: String, domainEvent: DomainEvent) {
    val domainEventAsString = objectMapper.writeValueAsString(domainEvent)
    val publishRequest = PublishRequest.builder().topicArn(domainEventsTopic?.arn)
      .message(domainEventAsString)
      .messageAttributes(
        mapOf(
          "eventType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(eventType).build(),
        ),
      ).build()

    publishOffenderEvent(publishRequest)?.get()

    assertDomainEventReceiverQueueHasProcessedMessages()
  }

  private fun publishOffenderEvent(publishRequest: PublishRequest): CompletableFuture<PublishResponse>? {
    return domainEventsTopic?.snsClient?.publish(publishRequest)
  }

  fun createDeliusDetailUrl(crn: String): String {
    val builder = StringBuilder()
    builder.append("https://domain-events-and-delius-dev.hmpps.service.justice.gov.uk/probation-case.engagement.created/")
    builder.append(crn)
    return builder.toString()
  }

  fun createNomsDetailUrl(nomsNUmber: String): String {
    val builder = StringBuilder()
    builder.append("https://prisoner-search-dev.prison.service.justice.gov.uk/prisoner/")
    builder.append(nomsNUmber)
    return builder.toString()
  }
}
