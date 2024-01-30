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
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NEW_LIBRA_CASE_RECEIVED

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
      MessageType.LIBRA_COURT_CASE -> processLibraHearingEvent(
        objectMapper.readValue<LibraHearingEvent>(
          sqsMessage.message,
        ),
      )

      MessageType.COMMON_PLATFORM_HEARING -> processCommonPlatformHearingEvent(
        objectMapper.readValue<CommonPlatformHearingEvent>(
          sqsMessage.message,
        ),
      )

      else -> {
        log.debug("Received case type ${MessageType.UNKNOWN.name}")
        telemetryService.trackEvent(TelemetryEventType.UNKNOWN_CASE_RECEIVED, emptyMap())
      }
    }
  }

  fun processLibraHearingEvent(libraHearingEvent: LibraHearingEvent) {
    log.debug("Processing LIBRA event")
    val person = Person.from(libraHearingEvent)
    telemetryService.trackEvent(
      NEW_LIBRA_CASE_RECEIVED,
      mapOf("PNC" to person.otherIdentifiers?.pncIdentifier?.pncId, "CRO" to person.otherIdentifiers?.cro),
    )

    courtCaseEventsService.processPersonFromCourtCaseEvent(person)
  }

  fun processCommonPlatformHearingEvent(commonPlatformHearingEvent: CommonPlatformHearingEvent) {
    log.debug("Processing COMMON PLATFORM event")

    val uniqueDefendants = commonPlatformHearingEvent.hearing.prosecutionCases
      .flatMap { it.defendants }
      .distinctBy {
        it.personDefendant?.personDetails?.firstName +
          it.personDefendant?.personDetails?.lastName +
          it.personDefendant?.personDetails?.dateOfBirth +
          it.pncId +
          it.croNumber
      }
    val pncValues = uniqueDefendants.joinToString(" ") { it.pncId.toString() }
    log.debug("Processing CP Event with ${uniqueDefendants.size} distinct defendants with pnc $pncValues")

    uniqueDefendants.forEach { defendant ->
      telemetryService.trackEvent(
        TelemetryEventType.NEW_CP_CASE_RECEIVED,
        mapOf("PNC" to defendant.pncId, "CRO" to defendant.croNumber),
      )
      courtCaseEventsService.processPersonFromCourtCaseEvent(Person.from(defendant))
    }
  }
}
