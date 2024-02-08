package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DELIUS_RECORD_CREATION_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NEW_DELIUS_RECORD_NEW_PNC
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.SECONDS

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

  @Test
  fun `should receive the message successfully when new offender event published`() {
    // Given
    val crn = "XXX1234"
    val expectedPncNumber = PNCIdentifier.create("2020/0476873U")
    val domainEvent = objectMapper.writeValueAsString(createDomainEvent(NEW_OFFENDER_CREATED, crn))

    val publishRequest = PublishRequest.builder().topicArn(domainEventsTopic?.arn)
      .message(domainEvent)
      .messageAttributes(
        mapOf(
          "eventType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(NEW_OFFENDER_CREATED).build(),
        ),
      ).build()

    // When publish new offender domain event with CRN XXX1234
    publishOffenderEvent(publishRequest)?.get()

    // Then
    assertNewOffenderDomainEventReceiverQueueHasProcessedMessages()

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(DELIUS_RECORD_CREATION_RECEIVED),
        org.mockito.kotlin.check {
          assertThat(it["CRN"]).isEqualTo(crn)
        },
      )
    }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(NEW_DELIUS_RECORD_NEW_PNC),
        org.mockito.kotlin.check {
          assertThat(it["CRN"]).isEqualTo(crn)
        },
      )
    }

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findPersonEntityByPncNumber(expectedPncNumber) }

    assertThat(personEntity.personId).isNotNull()
    assertThat(personEntity.offenders).hasSize(1)
    assertThat(personEntity.offenders[0].pncNumber).isEqualTo(expectedPncNumber)
    assertThat(personEntity.offenders[0].crn).isEqualTo(crn)
  }

  @Test
  @Disabled("TODO")
  fun `should write offender without PNC if PNC is invalid`() {
    // Given
    val crn = "XXX5678"
    val domainEvent = objectMapper.writeValueAsString(createDomainEvent(NEW_OFFENDER_CREATED, crn))

    val publishRequest = PublishRequest.builder().topicArn(domainEventsTopic?.arn)
      .message(domainEvent)
      .messageAttributes(
        mapOf(
          "eventType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(NEW_OFFENDER_CREATED).build(),
        ),
      ).build()

    // When
    publishOffenderEvent(publishRequest)?.get()

    // Then

    assertNewOffenderDomainEventReceiverQueueHasProcessedMessages()

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(DELIUS_RECORD_CREATION_RECEIVED),
        org.mockito.kotlin.check {
          assertThat(it["CRN"]).isEqualTo(crn)
        },
      )
    }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(NEW_DELIUS_RECORD_NEW_PNC),
        org.mockito.kotlin.check {
          assertThat(it["CRN"]).isEqualTo(crn)
        },
      )
    }

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByOffendersCrn(crn) }

    assertThat(personEntity.personId).isNotNull()
    assertThat(personEntity.offenders).hasSize(1)
    assertThat(personEntity.offenders[0].pncNumber?.pncId).isEqualTo("")
    assertThat(personEntity.offenders[0].crn).isEqualTo(crn)
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
