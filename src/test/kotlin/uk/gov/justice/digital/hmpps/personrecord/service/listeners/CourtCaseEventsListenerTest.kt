package uk.gov.justice.digital.hmpps.personrecord.service.listeners

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
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.CourtCaseEventsListener
import uk.gov.justice.digital.hmpps.personrecord.message.processor.CourtCaseEventsProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.MessageAttributes
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.helper.testMessage
import uk.gov.justice.digital.hmpps.personrecord.service.helper.testMessageWithUnknownType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import kotlin.test.assertFailsWith

@Suppress("INLINE_FROM_HIGHER_PLATFORM")
@ExtendWith(MockitoExtension::class)
class CourtCaseEventsListenerTest {
  @Mock
  private lateinit var courtCaseEventsProcessor: CourtCaseEventsProcessor

  @Mock
  private lateinit var telemetryService: TelemetryService

  private lateinit var courtCaseEventsListener: CourtCaseEventsListener

  private lateinit var sqsMessage: SQSMessage

  @BeforeEach
  fun setUp() {
    courtCaseEventsListener = CourtCaseEventsListener(
      objectMapper = ObjectMapper(),
      courtCaseEventsProcessor = courtCaseEventsProcessor,
      telemetryService = telemetryService,
    )
  }

  @Test
  fun `should call the processor with correct libra data`() {
    // given
    val rawMessage = testMessage(MessageType.LIBRA_COURT_CASE.name)
    sqsMessage = SQSMessage(
      type = "Notification",
      messageId = "5bc08be0-16e9-5da9-b9ec-d2c870a59bad",
      message = "{  \"caseId\": 1217464, \"hearingId\": \"hearing-id-one\",   \"caseNo\": \"1600032981\"}}",
      messageAttributes = MessageAttributes(MessageType.LIBRA_COURT_CASE),
    )
    // when
    courtCaseEventsListener.onMessage(rawMessage = rawMessage)

    // then
    verify(courtCaseEventsProcessor).processEvent(sqsMessage)
  }

  @Test
  fun `should call the processor with correct common platform data`() {
    // given
    val rawMessage = testMessage(MessageType.COMMON_PLATFORM_HEARING.name)
    sqsMessage = SQSMessage(
      type = "Notification",
      messageId = "5bc08be0-16e9-5da9-b9ec-d2c870a59bad",
      message = "{  \"caseId\": 1217464, \"hearingId\": \"hearing-id-one\",   \"caseNo\": \"1600032981\"}}",
      messageAttributes = MessageAttributes(MessageType.COMMON_PLATFORM_HEARING),
    )
    // when
    courtCaseEventsListener.onMessage(rawMessage = rawMessage)

    // then
    verify(courtCaseEventsProcessor).processEvent(sqsMessage)
  }

  @Test
  fun `should not call the processor and create telemetry event with wrong message type`() {
    // given
    val rawMessage = testMessageWithUnknownType(MessageType.COMMON_PLATFORM_HEARING.name)
    sqsMessage = SQSMessage(
      type = "Unknown",
      messageId = "5bc08be0-16e9-5da9-b9ec-d2c870a59bad",
      message = "{  \"caseId\": 1217464, \"hearingId\": \"hearing-id-one\",   \"caseNo\": \"1600032981\"}}",
      messageAttributes = MessageAttributes(MessageType.COMMON_PLATFORM_HEARING),
    )
    // when
    courtCaseEventsListener.onMessage(rawMessage = rawMessage)

    // then
    verify(telemetryService).trackEvent(TelemetryEventType.UNKNOWN_CASE_RECEIVED, mapOf("UNKNOWN_SOURCE_NAME" to sqsMessage.type))
    verifyNoInteractions(courtCaseEventsProcessor)
  }

  @Test
  fun `should create correct telemetry event when exception thrown`() {
    // given
    val rawMessage = testMessage(MessageType.COMMON_PLATFORM_HEARING.name)
    sqsMessage = SQSMessage(
      type = "Notification",
      messageId = "5bc08be0-16e9-5da9-b9ec-d2c870a59bad",
      message = "{  \"caseId\": 1217464, \"hearingId\": \"hearing-id-one\",   \"caseNo\": \"1600032981\"}}",
      messageAttributes = MessageAttributes(MessageType.COMMON_PLATFORM_HEARING),
    )
    whenever(courtCaseEventsProcessor.processEvent(any())).thenThrow(IllegalArgumentException("Something went wrong"))

    // when
    val exception = assertFailsWith<IllegalArgumentException>(
      block = { courtCaseEventsListener.onMessage(rawMessage = rawMessage) },
    )

    // then
    verify(telemetryService).trackEvent(TelemetryEventType.CASE_READ_FAILURE, mapOf("MESSAGE_ID" to sqsMessage.messageId))
  }
}
