package uk.gov.justice.digital.hmpps.personrecord.message.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.service.CourtCaseEventsService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class CourtCaseEventsProcessor(
  private val objectMapper: ObjectMapper,
  private val courtCaseEventsService: CourtCaseEventsService,
  private val telemetryService: TelemetryService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(sqsMessage: SQSMessage) {
    log.debug("Received message with id ${sqsMessage.messageId}")
    when (sqsMessage.getMessageType()) {
      MessageType.LIBRA_COURT_CASE -> processLibraHearingEvent(objectMapper.readValue<LibraHearingEvent>(sqsMessage.message))
      MessageType.COMMON_PLATFORM_HEARING -> processCommonPlatformHearingEvent(objectMapper.readValue<CommonPlatformHearingEvent>(sqsMessage.message))
      else -> {
        log.debug("Received case type ${MessageType.UNKNOWN.name}")
        telemetryService.trackEvent(TelemetryEventType.UNKNOWN_CASE_RECEIVED, emptyMap())
      }
    }
  }

  fun processLibraHearingEvent(libraHearingEvent: LibraHearingEvent) {
    log.debug("Processing LIBRA event")
    log.debug(libraHearingEvent.toString())
    telemetryService.trackEvent(TelemetryEventType.NEW_LIBRA_CASE_RECEIVED, mapOf("PNC" to libraHearingEvent.pnc, "CRO" to libraHearingEvent.cro))
    courtCaseEventsService.processPersonFromCourtCaseEvent(Person.from(libraHearingEvent))
  }

  fun processCommonPlatformHearingEvent(commonPlatformHearingEvent: CommonPlatformHearingEvent) {
    log.debug("Processing COMMON PLATFORM event")
    log.debug(commonPlatformHearingEvent.toString())
    commonPlatformHearingEvent.hearing.prosecutionCases.forEach { prosecutionCase ->
      prosecutionCase.defendants.forEach {
        telemetryService.trackEvent(TelemetryEventType.NEW_CP_CASE_RECEIVED, mapOf("PNC" to it.pncId, "CRO" to it.croNumber))
        courtCaseEventsService.processPersonFromCourtCaseEvent(Person.from(it))
      }
    }
  }
}
