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
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.PrisonDomainEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.messages.prisonDomainEvent
import java.util.UUID
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class PrisonDomainEventListenerTest {

  @Mock
  private lateinit var prisonEventProcessor: PrisonEventProcessor

  @Mock
  private lateinit var telemetryService: TelemetryService

  private lateinit var prisonerDomainEventListener: PrisonDomainEventListener

  @BeforeEach
  fun setUp() {
    prisonerDomainEventListener = PrisonDomainEventListener(
      objectMapper = ObjectMapper(),
      prisonEventProcessor = prisonEventProcessor,
      telemetryService = telemetryService,
    )
  }

  @Test
  fun `should create correct telemetry event when exception thrown`() {
    // given
    val prisonNumber = UUID.randomUUID().toString()
    val messageId = UUID.randomUUID().toString()
    val rawMessage = prisonDomainEvent(PRISONER_CREATED, prisonNumber, messageId = messageId)
    whenever(prisonEventProcessor.processEvent(any())).thenThrow(IllegalArgumentException("Something went wrong"))
    // when

    assertFailsWith<IllegalArgumentException>(
      block = { prisonerDomainEventListener.onDomainEvent(rawMessage = rawMessage) },
    )

    // then
    verify(telemetryService).trackEvent(
      TelemetryEventType.MESSAGE_PROCESSING_FAILED,
      mapOf(
        EventKeys.MESSAGE_ID to messageId,
        EventKeys.SOURCE_SYSTEM to "NOMIS",
        EventKeys.EVENT_TYPE to "prisoner-offender-search.prisoner.created",
      ),
    )
  }
}
