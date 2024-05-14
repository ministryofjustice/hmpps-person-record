package uk.gov.justice.digital.hmpps.personrecord.message.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.UNKNOWN
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_MESSAGE_RECEIVED

@Service
class CourtCaseEventsProcessor(
  private val objectMapper: ObjectMapper,
  private val personService: PersonService,
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(sqsMessage: SQSMessage) {
    log.debug("Received message with id ${sqsMessage.messageId}")
    when (sqsMessage.getMessageType()) {
      COMMON_PLATFORM_HEARING -> processCommonPlatformHearingEvent(
        objectMapper.readValue<CommonPlatformHearingEvent>(
          sqsMessage.message,
        ),
        sqsMessage.messageId,
      )
      else -> {
        if (sqsMessage.getMessageType()?.equals(UNKNOWN) == true) {
          log.debug("Received case type ${UNKNOWN.name}")
        }
      }
    }
  }

  fun processCommonPlatformHearingEvent(commonPlatformHearingEvent: CommonPlatformHearingEvent, messageId: String?) {
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
        HMCTS_MESSAGE_RECEIVED,
        mapOf(
          "PNC" to person.otherIdentifiers?.pncIdentifier.toString(),
          "CRO" to person.otherIdentifiers?.croIdentifier.toString(),
          "messageId" to messageId,
        ),
      )
      process(person)
    }
  }

  private fun process(person: Person) {
    try {
      personService.processPerson(person) {
        person.defendantId?.let {
          personRepository.findAllByDefendantId(it)
        }
      }
    } catch (e: Exception) {
      when (e) {
        is CannotAcquireLockException, is JpaSystemException -> {
          log.warn("Expected error when processing $e.message")
        }
        else -> throw e
      }
    }
  }
}
