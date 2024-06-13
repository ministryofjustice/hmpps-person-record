package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prisoner

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.PrisonerDomainEventsListener
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonEventsProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.messages.prisonerDomainEvent
import java.util.UUID
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class PrisonerDomainEventsListenerTest {

  @Mock
  private lateinit var prisonEventsProcessor: PrisonEventsProcessor

  @Mock
  private lateinit var telemetryService: TelemetryService

  private lateinit var prisonerDomainEventListener: PrisonerDomainEventsListener

  @BeforeEach
  fun setUp() {
    prisonerDomainEventListener = PrisonerDomainEventsListener(
      objectMapper = ObjectMapper(),
      prisonEventsProcessor = prisonEventsProcessor,
      telemetryService = telemetryService,
    )
  }

  @Test
  fun `should create correct telemetry event when exception thrown`() {
    // given
    val prisonNumber = UUID.randomUUID().toString()
    val messageId = UUID.randomUUID().toString()
    val rawMessage = prisonerDomainEvent(PRISONER_CREATED, prisonNumber, messageId = messageId)
    whenever(prisonEventsProcessor.processEvent(any())).thenThrow(IllegalArgumentException("Something went wrong"))
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
