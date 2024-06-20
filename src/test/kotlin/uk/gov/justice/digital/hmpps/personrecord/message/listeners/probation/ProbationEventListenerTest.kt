package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.ProbationEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.messages.probationEvent
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import java.util.UUID
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class ProbationEventListenerTest {

  @Mock
  private lateinit var offenderEventsProcessor: ProbationEventProcessor

  @Mock
  private lateinit var telemetryService: TelemetryService

  private lateinit var probationEventListener: ProbationEventListener

  @BeforeEach
  fun setUp() {
    probationEventListener = ProbationEventListener(
      objectMapper = ObjectMapper(),
      eventProcessor = offenderEventsProcessor,
      telemetryService = telemetryService,
    )
  }

  @Test
  fun `should create correct telemetry event when exception thrown`() {
    val crn = randomCRN()
    val messageId = UUID.randomUUID().toString()
    val rawMessage = probationEvent(NEW_OFFENDER_CREATED, crn, messageId = messageId)
    whenever(offenderEventsProcessor.processEvent(any())).thenThrow(IllegalArgumentException("Something went wrong"))

    assertFailsWith<IllegalArgumentException>(
      block = { probationEventListener.onDomainEvent(rawMessage = rawMessage) },
    )

    verify(telemetryService).trackEvent(
      TelemetryEventType.MESSAGE_PROCESSING_FAILED,
      mapOf(
        EventKeys.MESSAGE_ID to messageId,
        EventKeys.SOURCE_SYSTEM to "DELIUS",
        EventKeys.EVENT_TYPE to "probation-case.engagement.created",
      ),
    )
  }
}
