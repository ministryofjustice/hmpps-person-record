package uk.gov.justice.digital.hmpps.personrecord.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.verification.VerificationMode
import org.springframework.boot.builder.SpringApplicationBuilder
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.HmppsPersonRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.Duration

abstract class MultiNodeTestBase {

  internal val hmppsQueueService = getBean("hmppsQueueService", HmppsQueueService::class.java)

  internal fun <T> getBean(name: String, clazz: Class<T>) =
    instance1.context().getBean(name, clazz)

  internal val courtCaseEventsTopic =
    hmppsQueueService.findByTopicId("courtcaseeventstopic")

  internal val courtCaseEventsQueue =
    hmppsQueueService.findByQueueId("cprcourtcaseeventsqueue")

  internal val cprDeliusOffenderEventsQueue =
    hmppsQueueService.findByQueueId("cprdeliusoffendereventsqueue")

  internal val telemetryClient = getBean("telemetryClient", TelemetryClient::class.java)

  internal val personRepository: PersonRepository = instance1.context().getBean("personRepository", PersonRepository::class.java)

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

  @BeforeEach
  fun beforeEach() {
    personRepository.deleteAll()
    courtCaseEventsQueue?.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtCaseEventsQueue.dlqUrl).build(),
    ).get()
    courtCaseEventsQueue.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(courtCaseEventsQueue.queueUrl).build(),
    ).get()
    cprDeliusOffenderEventsQueue?.sqsClient?.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(cprDeliusOffenderEventsQueue.queueUrl).build(),
    )
    cprDeliusOffenderEventsQueue?.sqsDlqClient?.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(cprDeliusOffenderEventsQueue.dlqUrl).build(),
    )
  }

  companion object {
    internal val instance1: SpringApplicationBuilder = SpringApplicationBuilder(HmppsPersonRecord::class.java)
      .profiles("test", "test-instance-1")

    internal val instance2: SpringApplicationBuilder = SpringApplicationBuilder(HmppsPersonRecord::class.java)
      .profiles("test", "test-instance-2")

    @JvmStatic
    @BeforeAll
    fun beforeAll() {
      instance1.run()
      instance2.run()
    }

    @JvmStatic
    @AfterAll
    fun afterAll() {
      instance1.context().close()
      instance2.context().close()
    }
  }
}
