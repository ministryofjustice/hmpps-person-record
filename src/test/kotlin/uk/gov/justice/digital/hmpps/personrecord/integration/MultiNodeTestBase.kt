package uk.gov.justice.digital.hmpps.personrecord.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.builder.SpringApplicationBuilder
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.HmppsPersonRecord
import uk.gov.justice.hmpps.sqs.HmppsQueueService

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

  @BeforeEach
  fun beforeEach() {
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
