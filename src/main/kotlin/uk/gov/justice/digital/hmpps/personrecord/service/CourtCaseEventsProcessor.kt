package uk.gov.justice.digital.hmpps.personrecord.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent

@Service
class CourtCaseEventsProcessor(
  val objectMapper: ObjectMapper,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(sqsMessage: SQSMessage) {
    log.debug("Received message with id ${sqsMessage.messageId}")
    when (sqsMessage.getMessageType()) {
      MessageType.LIBRA_COURT_CASE -> processLibraHearingEvent(objectMapper.readValue<LibraHearingEvent>(sqsMessage.message))
      MessageType.COMMON_PLATFORM_HEARING -> processCommonPlatformHearingEvent(objectMapper.readValue<CommonPlatformHearingEvent>(sqsMessage.message))
      else -> { log.debug("Received case type ${MessageType.UNKNOWN.name}") }
    }
  }

  fun processLibraHearingEvent(libraHearingEvent: LibraHearingEvent) {
    log.debug("Processing LIBRA  event")
    log.debug(libraHearingEvent.toString())
  }

  fun processCommonPlatformHearingEvent(commonPlatformHearingEvent: CommonPlatformHearingEvent) {
    log.info("Processing COMMON PLATFORM  event")
    log.debug(commonPlatformHearingEvent.toString())
  }
}
