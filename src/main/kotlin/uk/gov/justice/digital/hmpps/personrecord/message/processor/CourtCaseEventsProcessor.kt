package uk.gov.justice.digital.hmpps.personrecord.message.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.UNKNOWN
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.service.CourtCaseEventsService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NEW_CP_CASE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NEW_LIBRA_CASE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.UNKNOWN_CASE_RECEIVED

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
      LIBRA_COURT_CASE -> processLibraHearingEvent(
        objectMapper.readValue<LibraHearingEvent>(
          sqsMessage.message,
        ),
      )

      COMMON_PLATFORM_HEARING -> processCommonPlatformHearingEvent(
        objectMapper.readValue<CommonPlatformHearingEvent>(
          sqsMessage.message,
        ),
      )

      else -> {
        log.debug("Received case type ${UNKNOWN.name}")
        telemetryService.trackEvent(UNKNOWN_CASE_RECEIVED, emptyMap())
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
      val person = Person.from(defendant)
      telemetryService.trackEvent(
        NEW_CP_CASE_RECEIVED,
        mapOf("PNC" to person.otherIdentifiers?.pncIdentifier?.pncId, "CRO" to person.otherIdentifiers?.cro),
      )
      try {
        courtCaseEventsService.processPersonFromCourtCaseEvent(person)
      } catch (e: CannotAcquireLockException) {
        log.warn("CannotAcquireLockException")
        telemetryService.trackEvent(
          TelemetryEventType.NEW_CASE_EXACT_MATCH,
          mapOf("PNC" to person.otherIdentifiers?.pncIdentifier?.pncId),
        )
      }
    }
  }
}
