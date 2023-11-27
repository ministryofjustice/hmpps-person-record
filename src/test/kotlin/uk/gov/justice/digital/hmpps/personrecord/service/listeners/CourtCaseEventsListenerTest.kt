package uk.gov.justice.digital.hmpps.personrecord.service.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.personrecord.model.MessageAttributes
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.service.CourtCaseEventsProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.helper.testMessage

@ExtendWith(MockitoExtension::class)
class CourtCaseEventsListenerTest {
  @Mock
  private lateinit var courtCaseEventsProcessor: CourtCaseEventsProcessor

  private lateinit var courtCaseEventsListener: CourtCaseEventsListener

  private lateinit var sqsMessage: SQSMessage

  @BeforeEach
  fun setUp() {
    courtCaseEventsListener = CourtCaseEventsListener(
      objectMapper = ObjectMapper(),
      courtCaseEventsProcessor = courtCaseEventsProcessor,
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
}
