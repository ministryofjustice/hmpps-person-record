package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.message.processors.delius.OffenderEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis.PrisonerEventsProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.offenderDomainEvent
import uk.gov.justice.digital.hmpps.personrecord.test.messages.prisonerDomainEvent
import java.util.UUID
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class OffenderDomainEventsListenerTest {

  @Mock
  private lateinit var offenderEventsProcessor: OffenderEventProcessor

  @Mock
  private lateinit var telemetryService: TelemetryService

  @Mock
  private lateinit var featureFlag: FeatureFlag

  private lateinit var offenderDomainEventsListener: OffenderDomainEventsListener

  @BeforeEach
  fun setUp() {
    offenderDomainEventsListener = OffenderDomainEventsListener(
      objectMapper = ObjectMapper(),
      eventProcessor = offenderEventsProcessor,
      telemetryService = telemetryService,
      featureFlag = featureFlag,
    )
  }

  @Test
  fun `should create correct telemetry event when exception thrown`() {
    // given
    val crn = UUID.randomUUID().toString()
    val messageId = UUID.randomUUID().toString()
    val rawMessage = offenderDomainEvent(NEW_OFFENDER_CREATED, crn, messageId = messageId)
    whenever(offenderEventsProcessor.processEvent(any())).thenThrow(IllegalArgumentException("Something went wrong"))
    // when

    assertFailsWith<IllegalArgumentException>(
      block = { offenderDomainEventsListener.onDomainEvent(rawMessage = rawMessage) },
    )

    // then
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
