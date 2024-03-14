package uk.gov.justice.digital.hmpps.personrecord.message.listeners

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
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.message.processor.CourtCaseEventsProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.MessageAttributes
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.LIBRA_COURT_CASE
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

  @Mock
  private lateinit var featureFlag: FeatureFlag

  private lateinit var courtCaseEventsListener: CourtCaseEventsListener

  private lateinit var sqsMessage: SQSMessage

  @BeforeEach
  fun setUp() {
    courtCaseEventsListener = CourtCaseEventsListener(
      objectMapper = ObjectMapper(),
      courtCaseEventsProcessor = courtCaseEventsProcessor,
      telemetryService = telemetryService,
      featureFlag = featureFlag,
    )
    whenever(featureFlag.isHmctsSQSEnabled()).thenReturn(true)
  }

  @Test
  fun `should call the processor with correct libra data`() {
    // given
    val rawMessage = testMessage(LIBRA_COURT_CASE.name)
    sqsMessage = SQSMessage(
      type = "Notification",
      messageId = "5bc08be0-16e9-5da9-b9ec-d2c870a59bad",
      message = "{  \"caseId\": 1217464, \"hearingId\": \"hearing-id-one\",   \"caseNo\": \"1600032981\"}}",
      messageAttributes = MessageAttributes(LIBRA_COURT_CASE),
    )
    // when
    courtCaseEventsListener.onMessage(rawMessage = rawMessage)

    // then
    verify(courtCaseEventsProcessor).processEvent(sqsMessage)
  }

  @Test
  fun `should not call the processor when type is unknown`() {
    // given
    val rawMessage = testMessageWithUnknownType(COMMON_PLATFORM_HEARING.name)
    sqsMessage = SQSMessage(
      type = "Unknown",
      messageId = "5bc08be0-16e9-5da9-b9ec-d2c870a59bad",
      message = "{  \"caseId\": 1217464, \"hearingId\": \"hearing-id-one\",   \"caseNo\": \"1600032981\"}}",
      messageAttributes = MessageAttributes(COMMON_PLATFORM_HEARING),
    )
    // when
    courtCaseEventsListener.onMessage(rawMessage = rawMessage)

    // then
    verifyNoInteractions(courtCaseEventsProcessor)
  }

  @Test
  fun `should create correct telemetry event when exception thrown`() {
    // given
    val rawMessage = testMessage(COMMON_PLATFORM_HEARING.name)
    sqsMessage = SQSMessage(
      type = "Notification",
      messageId = "5bc08be0-16e9-5da9-b9ec-d2c870a59bad",
      message = "{  \"caseId\": 1217464, \"hearingId\": \"hearing-id-one\",   \"caseNo\": \"1600032981\"}}",
      messageAttributes = MessageAttributes(COMMON_PLATFORM_HEARING),
    )
    whenever(courtCaseEventsProcessor.processEvent(any())).thenThrow(IllegalArgumentException("Something went wrong"))

    // when
    assertFailsWith<IllegalArgumentException>(
      block = { courtCaseEventsListener.onMessage(rawMessage = rawMessage) },
    )

    // then
    verify(telemetryService).trackEvent(TelemetryEventType.HMCTS_PROCESSING_FAILURE, mapOf("MESSAGE_ID" to sqsMessage.messageId))
  }

  @Test
  fun `should not call the processor when the feature flag is false`() {
    // given
    val rawMessage = testMessage(LIBRA_COURT_CASE.name)
    sqsMessage = SQSMessage(
      type = "Notification",
      messageId = "5bc08be0-16e9-5da9-b9ec-d2c870a59bad",
      message = "{  \"caseId\": 1217464, \"hearingId\": \"hearing-id-one\",   \"caseNo\": \"1600032981\"}}",
      messageAttributes = MessageAttributes(LIBRA_COURT_CASE),
    )
    whenever(featureFlag.isHmctsSQSEnabled()).thenReturn(false)
    // when
    courtCaseEventsListener.onMessage(rawMessage = rawMessage)

    // then
    verifyNoInteractions(courtCaseEventsProcessor)
    verifyNoInteractions(telemetryService)
  }
}
