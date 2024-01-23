package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.*
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private const val CRN = "XXX1234"

@Sql(
  scripts = ["classpath:sql/before-test.sql"],
  config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
  executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
@Sql(
  scripts = ["classpath:sql/after-test.sql"],
  config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
@Suppress("INLINE_FROM_HIGHER_PLATFORM")
class OffenderDomainEventsListenerIntTest : IntegrationTestBase() {

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
  }
  val cprDeliusOffenderEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprdeliusoffendereventsqueue")
  }

  @Autowired
  lateinit var personRepository: PersonRepository

  @BeforeEach
  fun setUp() {
    cprDeliusOffenderEventsQueue?.sqsClient?.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(cprDeliusOffenderEventsQueue?.queueUrl).build(),
    )
  }

  @Test
  fun `should receive the message successfully when new offender event published`() {
    // Given
    val expectedPncNumber = "PN/1234560XX"
    val domainEvent = objectMapper.writeValueAsString(createDomainEvent(NEW_OFFENDER_CREATED, CRN))

    val publishRequest = PublishRequest.builder().topicArn(domainEventsTopic?.arn)
      .message(domainEvent)
      .messageAttributes(
        mapOf(
          "eventType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(NEW_OFFENDER_CREATED).build(),
        ),
      ).build()

    // When publish new offender domain event with CRN XXX1234
    val publishResponse = publishOffenderEvent(publishRequest)?.get()

    // Then
    assertThat(publishResponse?.sdkHttpResponse()?.isSuccessful).isTrue()
    assertThat(publishResponse?.messageId()).isNotNull()

    assertNewOffenderDomainEventReceiverQueueHasProcessedMessages()

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(TelemetryEventType.DELIUS_RECORD_CREATION_RECEIVED),
        org.mockito.kotlin.check {
          assertThat(it["CRN"]).isEqualTo(CRN)
        },
      )
    }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(TelemetryEventType.NEW_DELIUS_RECORD_NEW_PNC),
        org.mockito.kotlin.check {
          assertThat(it["CRN"]).isEqualTo(CRN)
        },
      )
    }

    val personEntity = await.atMost(10, TimeUnit.SECONDS) untilNotNull { personRepository.findPersonEntityByPncNumber(expectedPncNumber) }

    assertThat(personEntity.personId).isNotNull()
    assertThat(personEntity.offenders).hasSize(1)
    assertThat(personEntity.offenders[0].pncNumber).isEqualTo(expectedPncNumber)
    assertThat(personEntity.offenders[0].crn).isEqualTo(CRN)
  }

  fun publishOffenderEvent(publishRequest: PublishRequest): CompletableFuture<PublishResponse>? {
    return domainEventsTopic?.snsClient?.publish(publishRequest)
  }

  fun createDomainEvent(eventType: String, crn: String): DomainEvent {
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    return DomainEvent(eventType = eventType, detailUrl = createDetailUrl(crn), personReference = personReference)
  }

  fun createDetailUrl(crn: String): String {
    val builder = StringBuilder()
    builder.append("https://domain-events-and-delius-dev.hmpps.service.justice.gov.uk/probation-case.engagement.created/")
    builder.append(crn)
    return builder.toString()
  }

  fun assertNewOffenderDomainEventReceiverQueueHasProcessedMessages() {
    await untilCallTo {
      cprDeliusOffenderEventsQueue?.sqsClient?.countMessagesOnQueue(cprDeliusOffenderEventsQueue!!.queueUrl)
        ?.get()
    } matches { it == 0 }
  }
}
