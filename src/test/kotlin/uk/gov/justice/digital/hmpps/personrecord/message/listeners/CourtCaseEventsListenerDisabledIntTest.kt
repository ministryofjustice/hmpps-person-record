package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.jmock.lib.concurrent.Blitzer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingSingleNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearingWithSameDefendantIdTwice
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.UUID.randomUUID

@ActiveProfiles("seeding")
@Disabled("This works when run in isolation but not as part of check.")
class CourtCaseEventsListenerDisabledIntTest : MessagingSingleNodeTestBase() {

  @Test
  fun `should not process any messages`() {
    val pncNumber = PNCIdentifier.from("2003/0062845E")
    val defendantId = randomUUID().toString()
    buildPublishRequest(defendantId, pncNumber)
    val blitzer = Blitzer(50, 5)
    try {
      blitzer.blitz {
        courtCaseEventsTopic?.snsClient?.publish(buildPublishRequest(defendantId, pncNumber))?.get()
      }
    } finally {
      blitzer.shutdown()
    }

    await untilCallTo {
      courtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(courtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 50 }

    await untilCallTo {
      courtCaseEventsQueue?.sqsDlqClient?.countMessagesOnQueue(courtCaseEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
  }

  private fun buildPublishRequest(
    defendantId: String,
    pncNumber: PNCIdentifier,
  ): PublishRequest? = PublishRequest.builder()
    .topicArn(courtCaseEventsTopic?.arn)
    .message(commonPlatformHearingWithSameDefendantIdTwice(defendantId = defendantId, pncNumber = pncNumber.pncId))
    .messageAttributes(
      mapOf(
        "messageType" to MessageAttributeValue.builder().dataType("String")
          .stringValue(COMMON_PLATFORM_HEARING.name).build(),
      ),
    )
    .build()
}
