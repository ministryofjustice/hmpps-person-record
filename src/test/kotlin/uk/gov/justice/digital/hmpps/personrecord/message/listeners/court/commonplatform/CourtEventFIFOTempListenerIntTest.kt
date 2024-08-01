package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.commonplatform

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.FIFO_DEFENDANT_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.FIFO_HEARING_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.LibraMessage
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateOfBirth
import uk.gov.justice.digital.hmpps.personrecord.test.randomHearingId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import java.time.format.DateTimeFormatter
import java.util.UUID.randomUUID

class CourtEventFIFOTempListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should log telemetry for Common Platform event consumed from FIFO queue`() {
    val firstDefendantId = randomUUID().toString()
    val firstPnc = randomPnc()
    val messageId = publishCourtMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(pnc = firstPnc, defendantId = firstDefendantId))), COMMON_PLATFORM_HEARING, topic = courtEventsTopic?.arn!!)

    checkTelemetry(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to firstDefendantId,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "FIFO" to "false",
      ),
    )
    checkTelemetry(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to firstDefendantId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "FIFO" to "true",
      ),
    )
  }

  @Test
  fun `should log telemetry for Common Platform event consumed from FIFO queue once after publishing the same message twice to the FIFO topic`() {
    val firstPnc = randomPnc()
    val firstName: String = randomName()
    val lastName: String = randomName()
    val cro: String = randomCro()
    val defendantId: String = randomUUID().toString()
    val hearingId: String = randomHearingId()
    val nationalInsuranceNumber: String = randomNationalInsuranceNumber()

    val commonPlatformHearingSetup = CommonPlatformHearingSetup(defendantId = defendantId, pnc = firstPnc, firstName = firstName, lastName = lastName, cro = cro, nationalInsuranceNumber = nationalInsuranceNumber, hearingId = hearingId)

//    publishing message twice
    publishCourtMessage(commonPlatformHearing(listOf(commonPlatformHearingSetup)), COMMON_PLATFORM_HEARING, topic = courtEventsTopic?.arn!!)
    publishCourtMessage(commonPlatformHearing(listOf(commonPlatformHearingSetup)), COMMON_PLATFORM_HEARING, topic = courtEventsTopic?.arn!!)

    checkTelemetry(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to defendantId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "FIFO" to "false",
      ),
      2,
    )
    checkTelemetry(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to defendantId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "FIFO" to "true",
      ),
      1,
    )
    checkTelemetry(
      FIFO_HEARING_CREATED,
      mapOf(
        "HEARING_ID" to hearingId,
      ),
      1,
    )
  }

  @Test
  fun `should log telemetry for LIBRA event consumed from FIFO queue`() {
    telemetryRepository.deleteAll()
    val libraMessage = LibraMessage(firstName = randomName(), lastName = randomName(), dateOfBirth = randomDateOfBirth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cro = "", pncNumber = "")
    val messageId = publishCourtMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

    checkTelemetry(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to LIBRA.name,
        "FIFO" to "false",
      ),
    )
    checkTelemetry(
      FIFO_DEFENDANT_RECEIVED,
      mapOf(

        "SOURCE_SYSTEM" to LIBRA.name,
        "FIFO" to "true",
      ),
    )
  }
}
