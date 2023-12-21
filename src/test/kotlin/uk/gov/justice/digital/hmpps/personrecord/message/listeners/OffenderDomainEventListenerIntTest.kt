package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.notifiers.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.PersonReference
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.concurrent.CompletableFuture

private const val CRN = "XXX1234"

class OffenderDomainEventListenerIntTest : IntegrationTestBase() {

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
  }
  val cprDeliusOffenderEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprdeliusoffendereventsqueue")
  }

  @BeforeEach
  fun setUp() {
    cprDeliusOffenderEventsQueue?.sqsClient?.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(cprDeliusOffenderEventsQueue?.queueUrl).build(),
    )
  }

  @Test
  fun `should receive the message successfully when new offender event published`() {
    // Given

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
