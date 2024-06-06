package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingSingleNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearingWithOneDefendant
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.UUID.randomUUID

@ActiveProfiles("seeding")
@Disabled("This works when run in isolation but not as part of check.")
class CourtCaseEventsListenerDisabledIntTest : MessagingSingleNodeTestBase() {

  @Test
  fun `should not process any messages`() {
    val defendantId = randomUUID().toString()
    val messageId = publishHMCTSMessage(commonPlatformHearingWithOneDefendant(defendantId = defendantId, pncNumber = "19810154257C"), COMMON_PLATFORM_HEARING)
    await untilCallTo {
      courtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(courtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 1 }

    checkTelemetry(
      HMCTS_MESSAGE_RECEIVED,
      mapOf("PNC" to "1981/0154257C", "MESSAGE_ID" to messageId),
      0,
    )
  }
}
