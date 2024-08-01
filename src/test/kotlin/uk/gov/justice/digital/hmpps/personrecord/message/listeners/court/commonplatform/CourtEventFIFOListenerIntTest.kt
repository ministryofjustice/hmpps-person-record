package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.commonplatform

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.FIFO_DEFENDANT_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import java.util.UUID.randomUUID

class CourtEventFIFOListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should log telemetry for event consumed from FIFO queue`() {
    val firstDefendantId = randomUUID().toString()
    val firstPnc = randomPnc()
    val messageId = publishCourtMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(pnc = firstPnc, defendantId = firstDefendantId))), COMMON_PLATFORM_HEARING, topic = courtEventsFIFOTopic?.arn!!)

    checkTelemetry(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to firstDefendantId,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "FIFO" to "true",
      ),
    )
  }

  @Test
  fun `When two identical messages are published to FIFO topic, application only consumes one `() {
    val defendantId = randomUUID().toString()
    val firstPnc = randomPnc()
    val firstName = randomName()
    val lastName = randomName()
    val cro = randomCro()
    val nationalInsuranceNumber = randomNationalInsuranceNumber()
    val firstMessageId = publishCourtMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            pnc = firstPnc,
            firstName = firstName,
            lastName = lastName,
            cro = cro,
            defendantId = defendantId,
            nationalInsuranceNumber = nationalInsuranceNumber,

          ),
        ),
      ),
      COMMON_PLATFORM_HEARING,
      topic = courtEventsFIFOTopic?.arn!!,
    )
    val secondMessageId = publishCourtMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            pnc = firstPnc,
            firstName = firstName,
            lastName = lastName,
            cro = cro,
            defendantId = defendantId,
            nationalInsuranceNumber = nationalInsuranceNumber,

          ),
        ),
      ),
      COMMON_PLATFORM_HEARING,
      topic = courtEventsFIFOTopic?.arn!!,
    )

    checkTelemetry(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to defendantId,
        "MESSAGE_ID" to firstMessageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "FIFO" to "true",
      ),
    )

    checkTelemetry(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to defendantId,
        "MESSAGE_ID" to secondMessageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "FIFO" to "true",
      ),
      0,
    )
  }

  @Test
  fun `When two different messages are published to FIFO topic, application consumes both `() {
    val firstDefendantId = randomUUID().toString()
    val secondDefendantId = randomUUID().toString()
    val firstPnc = randomPnc()
    val firstName = randomName()
    val lastName = randomName()
    val cro = randomCro()
    val nationalInsuranceNumber = randomNationalInsuranceNumber()
    val firstMessageId = publishCourtMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            pnc = firstPnc,
            firstName = firstName,
            lastName = lastName,
            cro = cro,
            defendantId = firstDefendantId,
            nationalInsuranceNumber = nationalInsuranceNumber,

          ),
        ),
      ),
      COMMON_PLATFORM_HEARING,
      topic = courtEventsFIFOTopic?.arn!!,
    )
    val secondMessageId = publishCourtMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            pnc = firstPnc,
            firstName = firstName,
            lastName = lastName,
            cro = cro,
            defendantId = secondDefendantId,
            nationalInsuranceNumber = nationalInsuranceNumber,

          ),
        ),
      ),
      COMMON_PLATFORM_HEARING,
      topic = courtEventsFIFOTopic?.arn!!,
    )

    checkTelemetry(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to firstDefendantId,
        "MESSAGE_ID" to firstMessageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "FIFO" to "true",
      ),
    )

    checkTelemetry(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to secondDefendantId,
        "MESSAGE_ID" to secondMessageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "FIFO" to "true",
      ),
    )
  }
}
