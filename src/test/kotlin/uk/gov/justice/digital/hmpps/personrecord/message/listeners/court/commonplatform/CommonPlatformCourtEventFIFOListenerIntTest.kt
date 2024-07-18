package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.commonplatform

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import java.util.UUID.randomUUID

class CommonPlatformCourtEventFIFOListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should log telemetry for event consumed from FIFO queue`() {
    val firstDefendantId = randomUUID().toString()
    val firstPnc = randomPnc()
    val messageId = publishCourtMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = firstDefendantId, pnc = firstPnc))), COMMON_PLATFORM_HEARING)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to firstDefendantId,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "FIFO" to "true",
      ),
    )
  }
}
