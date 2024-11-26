package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import java.util.UUID

@Component
class CourtEventProcessor(
  private val objectMapper: ObjectMapper,
  private val createUpdateService: CreateUpdateService,
  private val personService: PersonService,
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(sqsMessage: SQSMessage) {
    when (val messageType = sqsMessage.getMessageType()) {
      COMMON_PLATFORM_HEARING.name -> processCommonPlatformHearingEvent(sqsMessage)
      LIBRA_COURT_CASE.name -> processLibraEvent(sqsMessage)
      else -> {
        log.debug("Received case type $messageType")
      }
    }
  }

  private fun processCommonPlatformHearingEvent(sqsMessage: SQSMessage) {
    val commonPlatformHearingEvent = objectMapper.readValue<CommonPlatformHearingEvent>(
      sqsMessage.message,
    )

    val uniqueDefendants = commonPlatformHearingEvent.hearing.prosecutionCases
      .flatMap { it.defendants }
      .filterNot { it.isYouth }
      .distinctBy {
        it.personDefendant?.personDetails?.firstName +
          it.personDefendant?.personDetails?.lastName +
          it.personDefendant?.personDetails?.dateOfBirth +
          it.pncId +
          it.cro
      }
    val defendantIDs = uniqueDefendants.joinToString(" ") { it.id.toString() }
    log.debug("Processing Common Platform Event with ${uniqueDefendants.size} distinct defendants with defendantId $defendantIDs")

    uniqueDefendants.forEach { defendant ->
      val person = Person.from(defendant)
      telemetryService.trackEvent(
        MESSAGE_RECEIVED,
        mapOf(
          EventKeys.DEFENDANT_ID to person.defendantId,
          EventKeys.EVENT_TYPE to COMMON_PLATFORM_HEARING.name,
          EventKeys.MESSAGE_ID to sqsMessage.messageId,
          EventKeys.SOURCE_SYSTEM to SourceSystemType.COMMON_PLATFORM.name,
        ),
      )
      createUpdateService.processMessage(person) {
        person.defendantId?.let {
          personRepository.findByDefendantId(it)
        }
      }
    }
  }

  private fun processLibraEvent(sqsMessage: SQSMessage) {
    val libraHearingEvent = objectMapper.readValue<LibraHearingEvent>(sqsMessage.message)
    val person = Person.from(libraHearingEvent)

    telemetryService.trackEvent(
      MESSAGE_RECEIVED,
      mapOf(
        EventKeys.PNC to person.references.getType(IdentifierType.PNC).toString(),
        EventKeys.CRO to person.references.getType(IdentifierType.CRO).toString(),
        EventKeys.EVENT_TYPE to LIBRA_COURT_CASE.name,
        EventKeys.MESSAGE_ID to sqsMessage.messageId,
        EventKeys.SOURCE_SYSTEM to SourceSystemType.LIBRA.name,
      ),
    )
    createUpdateService.processMessage(person) {
      val personEntity = personService.searchBySourceSystem(person)
      person.defendantId = personEntity?.defendantId ?: UUID.randomUUID().toString()
      return@processMessage personEntity
    }
  }
}
