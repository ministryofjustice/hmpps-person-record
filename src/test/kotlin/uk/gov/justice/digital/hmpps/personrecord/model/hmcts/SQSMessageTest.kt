package uk.gov.justice.digital.hmpps.personrecord.model.hmcts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.service.helper.testMessage

class SQSMessageTest {

  private lateinit var objectMapper: ObjectMapper

  @BeforeEach
  fun setUp() {
    objectMapper = jacksonObjectMapper().findAndRegisterModules()
  }

  @Test
  fun `should convert to sqs message of type libra from json`() {
    // given
    val rawMessage = testMessage(MessageType.LIBRA_COURT_CASE.name)
    // when
    val sqsMessage = objectMapper.readValue(rawMessage, SQSMessage::class.java)
    // then
    assertThat(sqsMessage).isNotNull()
    assertThat(sqsMessage.getMessageType()).isEqualTo(MessageType.LIBRA_COURT_CASE)
  }

  @Test
  fun `should convert to sqs message of type common platform from json`() {
    // given
    val rawMessage = testMessage(MessageType.COMMON_PLATFORM_HEARING.name)
    // when
    val sqsMessage = objectMapper.readValue(rawMessage, SQSMessage::class.java)
    // then
    assertThat(sqsMessage).isNotNull()
    assertThat(sqsMessage.getMessageType()).isEqualTo(MessageType.COMMON_PLATFORM_HEARING)
  }

  @Test
  fun `should convert to sqs message of type unknown from json`() {
    // given
    val rawMessage = testMessage(null)
    // when
    val sqsMessage = objectMapper.readValue(rawMessage, SQSMessage::class.java)
    // then
    assertThat(sqsMessage).isNotNull()
    assertThat(sqsMessage.getMessageType()).isEqualTo(MessageType.UNKNOWN)
  }
}
