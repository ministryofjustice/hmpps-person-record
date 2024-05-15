package uk.gov.justice.digital.hmpps.personrecord.integration

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.jmock.lib.concurrent.Blitzer
import org.junit.jupiter.api.Test
import org.springframework.boot.builder.SpringApplicationBuilder
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.HmppsPersonRecord
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithSameDefendantIdTwice
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class MultiNodeTestBase {

  @Test
  fun `should handle requests from 2 instances`() {
    val instance1: SpringApplicationBuilder = SpringApplicationBuilder(HmppsPersonRecord::class.java)
      .profiles("test", "test-instance-1")
    instance1.run()

    val courtCaseEventsTopic =
      (instance1.context().getBean("hmppsQueueService") as HmppsQueueService)
        .findByTopicId("courtcaseeventstopic")

    val courtCaseEventsQueue =
      (instance1.context().getBean("hmppsQueueService") as HmppsQueueService)
        .findByQueueId("cprcourtcaseeventsqueue")
    val cprDeliusOffenderEventsQueue =
      (instance1.context().getBean("hmppsQueueService") as HmppsQueueService)
        .findByQueueId("cprdeliusoffendereventsqueue")

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

    val instance2: SpringApplicationBuilder = SpringApplicationBuilder(HmppsPersonRecord::class.java)
      .profiles("test", "test-instance-2")
    instance2.run()

    // given
    val pncNumber = PNCIdentifier.from("2003/0062845E")

    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(commonPlatformHearingWithSameDefendantIdTwice(pncNumber = pncNumber.pncId))
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(COMMON_PLATFORM_HEARING.name).build(),
        ),
      )
      .build()
    // when
    val blitzer = Blitzer(100, 10)
    try {
      blitzer.blitz {
        courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()
      }
    } finally {
      blitzer.shutdown()
    }

    // then
    await untilCallTo {
      courtCaseEventsQueue.sqsClient.countMessagesOnQueue(courtCaseEventsQueue.queueUrl).get()
    } matches { it == 0 }

    await untilCallTo {
      courtCaseEventsQueue.sqsDlqClient?.countMessagesOnQueue(courtCaseEventsQueue.dlqUrl!!)?.get()
    } matches { it == 0 }

    instance1.context().close()
    instance2.context().close()
  }
}
