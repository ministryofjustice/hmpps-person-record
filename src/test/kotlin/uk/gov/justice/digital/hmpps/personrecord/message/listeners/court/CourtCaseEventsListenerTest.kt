package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.CourtCaseEventsListener
import uk.gov.justice.digital.hmpps.personrecord.message.processors.hmcts.CourtCaseEventsProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.messages.testMessage
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class CourtCaseEventsListenerTest {
  @Mock
  private lateinit var courtCaseEventsProcessor: CourtCaseEventsProcessor

  @Mock
  private lateinit var telemetryService: TelemetryService

  private lateinit var courtCaseEventsListener: CourtCaseEventsListener

  @BeforeEach
  fun setUp() {
    courtCaseEventsListener = CourtCaseEventsListener(
      objectMapper = ObjectMapper(),
      courtCaseEventsProcessor = courtCaseEventsProcessor,
      telemetryService = telemetryService,
    )
  }

  @Test
  fun `should not call the processor when type is unknown`() {
    val rawMessage = testMessage(COMMON_PLATFORM_HEARING.name, "Unknown")

    courtCaseEventsListener.onMessage(rawMessage = rawMessage)

    verifyNoInteractions(courtCaseEventsProcessor)
  }

  @Test
  fun `should create correct telemetry event when exception thrown`() {
    val rawMessage = testMessage(COMMON_PLATFORM_HEARING.name)
    whenever(courtCaseEventsProcessor.processEvent(any())).thenThrow(IllegalArgumentException("Something went wrong"))

    assertFailsWith<IllegalArgumentException>(
      block = { courtCaseEventsListener.onMessage(rawMessage = rawMessage) },
    )

    verify(telemetryService).trackEvent(
      TelemetryEventType.MESSAGE_PROCESSING_FAILED,
      mapOf(
        EventKeys.MESSAGE_ID to "5bc08be0-16e9-5da9-b9ec-d2c870a59bad",
        EventKeys.EVENT_TYPE to COMMON_PLATFORM_HEARING.name,
        EventKeys.SOURCE_SYSTEM to "HMCTS",
      ),
    )
  }
}
