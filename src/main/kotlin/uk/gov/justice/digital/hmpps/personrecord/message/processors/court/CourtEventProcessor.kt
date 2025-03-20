package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.message.TransactionalProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED

@Component
class CourtEventProcessor(
  private val objectMapper: ObjectMapper,
  private val transactionalProcessor: TransactionalProcessor,
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
    val commonPlatformHearing: String = sqsMessage.message
    val commonPlatformHearingEvent = objectMapper.readValue<CommonPlatformHearingEvent>(commonPlatformHearing)

    val uniquePersonDefendants = commonPlatformHearingEvent.hearing.prosecutionCases
      .flatMap { it.defendants }
      .filterNot { it.isYouth }
      .filter { it.isPerson() }
      .distinctBy { it.id }

    uniquePersonDefendants.forEach { defendant ->
      processCommonPlatformPerson(defendant, sqsMessage)
    }
  }

  private fun processCommonPlatformPerson(defendant: Defendant, sqsMessage: SQSMessage) {
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
    transactionalProcessor.processMessage(person) {
      person.defendantId?.let {
        personRepository.findByDefendantId(it)
      }
    }
  }

  private fun processLibraEvent(sqsMessage: SQSMessage) {
    val libraHearingEvent = objectMapper.readValue<LibraHearingEvent>(sqsMessage.message)
    when {
      isLibraPerson(libraHearingEvent) -> processLibraPerson(libraHearingEvent, sqsMessage)
    }
  }

  private fun processLibraPerson(libraHearingEvent: LibraHearingEvent, sqsMessage: SQSMessage) {
    val person = Person.from(libraHearingEvent)
    telemetryService.trackEvent(
      MESSAGE_RECEIVED,
      mapOf(
        EventKeys.C_ID to person.cId,
        EventKeys.EVENT_TYPE to LIBRA_COURT_CASE.name,
        EventKeys.MESSAGE_ID to sqsMessage.messageId,
        EventKeys.SOURCE_SYSTEM to SourceSystemType.LIBRA.name,
      ),
    )
    transactionalProcessor.processMessage(person) {
      person.cId?.let {
        personRepository.findByCId(it)
      }
    }
  }

  private fun isLibraPerson(libraHearingEvent: LibraHearingEvent) = libraHearingEvent.defendantType == DefendantType.PERSON.value
}
