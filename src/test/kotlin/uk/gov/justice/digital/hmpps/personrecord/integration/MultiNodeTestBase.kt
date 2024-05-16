package uk.gov.justice.digital.hmpps.personrecord.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.verification.VerificationMode
import org.springframework.boot.builder.SpringApplicationBuilder
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.HmppsPersonRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Duration

abstract class MultiNodeTestBase {

  internal val courtCaseEventsTopic =
    (instance1.context().getBean("hmppsQueueService") as HmppsQueueService)
      .findByTopicId("courtcaseeventstopic")

  internal val courtCaseEventsQueue =
    (instance1.context().getBean("hmppsQueueService") as HmppsQueueService)
      .findByQueueId("cprcourtcaseeventsqueue")

  internal val cprDeliusOffenderEventsQueue =
    (instance1.context().getBean("hmppsQueueService") as HmppsQueueService)
      .findByQueueId("cprdeliusoffendereventsqueue")

  internal val telemetryClient = (instance1.context().getBean("telemetryClient") as TelemetryClient)

  internal val personRepository: PersonRepository = (instance1.context().getBean("personRepository") as PersonRepository)

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
