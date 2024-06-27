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
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.CourtEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.processors.court.CourtEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.messages.testMessage
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class CourtEventListenerTest {
  @Mock
  private lateinit var courtEventProcessor: CourtEventProcessor

  @Mock
  private lateinit var telemetryService: TelemetryService

  private lateinit var courtEventListener: CourtEventListener

  @BeforeEach
  fun setUp() {
    courtEventListener = CourtEventListener(
      objectMapper = ObjectMapper(),
      courtEventProcessor = courtEventProcessor,
      telemetryService = telemetryService,
    )
  }

  @Test
  fun `should not call the processor when type is unknown`() {
    val rawMessage = testMessage(COMMON_PLATFORM_HEARING.name, "Unknown")

    courtEventListener.onMessage(rawMessage = rawMessage)

    verifyNoInteractions(courtEventProcessor)
  }

  @Test
  fun `should create correct telemetry event when exception thrown`() {
    val rawMessage = testMessage(COMMON_PLATFORM_HEARING.name)
    whenever(courtEventProcessor.processEvent(any())).thenThrow(IllegalArgumentException("Something went wrong"))

    assertFailsWith<IllegalArgumentException>(
      block = { courtEventListener.onMessage(rawMessage = rawMessage) },
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
