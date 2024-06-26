package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.PrisonEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.prisonEvent
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class PrisonEventListenerTest {

  @Mock
  private lateinit var prisonEventProcessor: PrisonEventProcessor

  @Mock
  private lateinit var telemetryService: TelemetryService

  private lateinit var prisonerDomainEventListener: PrisonEventListener

  @BeforeEach
  fun setUp() {
    prisonerDomainEventListener = PrisonEventListener(
      objectMapper = ObjectMapper(),
      prisonEventProcessor = prisonEventProcessor,
      telemetryService = telemetryService,
    )
  }

  @Test
  fun `should create correct telemetry event when exception thrown`() {
    val prisonNumber = randomPrisonNumber()
    val messageId = UUID.randomUUID().toString()
    val rawMessage = prisonEvent(PRISONER_CREATED, prisonNumber, messageId = messageId)
    whenever(prisonEventProcessor.processEvent(any())).thenThrow(IllegalArgumentException("Something went wrong"))

    assertFailsWith<IllegalArgumentException>(
      block = { prisonerDomainEventListener.onDomainEvent(rawMessage = rawMessage) },
    )

    verify(telemetryService).trackEvent(
      MESSAGE_PROCESSING_FAILED,
      mapOf(
        EventKeys.MESSAGE_ID to messageId,
        EventKeys.SOURCE_SYSTEM to "NOMIS",
        EventKeys.EVENT_TYPE to "prisoner-offender-search.prisoner.created",
      ),
    )
  }
}
