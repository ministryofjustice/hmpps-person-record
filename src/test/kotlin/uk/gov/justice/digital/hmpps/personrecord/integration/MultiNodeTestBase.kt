package uk.gov.justice.digital.hmpps.personrecord.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.verification.VerificationMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis.PrisonerCreatedEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis.PrisonerUpdatedEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.security.JwtAuthHelper
import uk.gov.justice.digital.hmpps.personrecord.service.PrisonerDomainEventService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.Duration

@ExtendWith(MultiApplicationContextExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class MultiNodeTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  internal lateinit var jwtHelper: JwtAuthHelper

  @SpyBean
  lateinit var prisonerCreatedEventProcessor: PrisonerCreatedEventProcessor

  @SpyBean
  lateinit var prisonerUpdatedEventProcessor: PrisonerUpdatedEventProcessor

  @SpyBean
  lateinit var prisonerDomainEventService: PrisonerDomainEventService

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
  }

  internal val courtCaseEventsTopic by lazy {
    hmppsQueueService.findByTopicId("courtcaseeventstopic")
  }

  val courtCaseEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprcourtcaseeventsqueue")
  }
  val offenderEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprdeliusoffendereventsqueue")
  }

  @SpyBean
  lateinit var telemetryClient: TelemetryClient

  @Autowired
  lateinit var personRepository: PersonRepository

  internal fun checkTelemetry(
    event: TelemetryEventType,
    expected: Map<String, String>,
    verificationMode: VerificationMode = times(1),
  ) {
    await.atMost(Duration.ofSeconds(3)) untilAsserted {
      verify(telemetryClient, verificationMode).trackEvent(
        eq(event.eventName),
        org.mockito.kotlin.check {
          assertThat(it).containsAllEntriesOf(expected)
        },
        eq(null),
      )
    }
  }

  internal fun publishHMCTSMessage(message: String, messageType: MessageType): String {
    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(message)
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(messageType.name).build(),
          "messageId" to MessageAttributeValue.builder().dataType("String")
            .stringValue("d3242a9f-c1cd-4d16-bd46-b7d33ccc9849").build(),
        ),
      )
      .build()

    val response: PublishResponse? = courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()

    expectNoMessagesOn(courtCaseEventsQueue)
    return response!!.messageId()
  }

  private fun expectNoMessagesOn(queue: HmppsQueue?) {
    await untilCallTo {
      queue?.sqsClient?.countMessagesOnQueue(queue.queueUrl)?.get()
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

    domainEventsTopic?.snsClient?.publish(publishRequest)?.get()

    expectNoMessagesOn(offenderEventsQueue)
  }

  fun createDeliusDetailUrl(crn: String): String =
    "https://domain-events-and-delius-dev.hmpps.service.justice.gov.uk/probation-case.engagement.created/$crn"

  fun createNomsDetailUrl(nomsNumber: String): String =
    "https://prisoner-search-dev.prison.service.justice.gov.uk/prisoner/$nomsNumber"

  internal fun WebTestClient.RequestHeadersSpec<*>.authorised(): WebTestClient.RequestBodySpec {
    val bearerToken = jwtHelper.createJwt(
      subject = "hmpps-person-record",
      expiryTime = Duration.ofMinutes(1L),
      roles = listOf(),
    )
    return header("authorization", "Bearer $bearerToken") as WebTestClient.RequestBodySpec
  }

  @BeforeEach
  fun beforeEach() {
    personRepository.deleteAll()
    courtCaseEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtCaseEventsQueue!!.dlqUrl).build(),
    ).get()
    courtCaseEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtCaseEventsQueue!!.queueUrl).build(),
    ).get()
    offenderEventsQueue?.sqsClient?.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(offenderEventsQueue!!.queueUrl).build(),
    )
    offenderEventsQueue?.sqsDlqClient?.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(offenderEventsQueue!!.dlqUrl).build(),
    )
  }
}
