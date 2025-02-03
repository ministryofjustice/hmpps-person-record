package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.smithy.kotlin.runtime.content.decodeToString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
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
import uk.gov.justice.digital.hmpps.personrecord.service.message.TransactionalProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.search.SearchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import java.util.UUID

@Component
class CourtEventProcessor(
  private val objectMapper: ObjectMapper,
  private val transactionalProcessor: TransactionalProcessor,
  private val searchService: SearchService,
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val s3Client: S3Client,
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
    val commonPlatformHearing: String = when {
      sqsMessage.getEventType() == "commonplatform.large.case.received" -> runBlocking { getPayloadFromS3(sqsMessage) }
      else -> sqsMessage.message
    }
    val commonPlatformHearingEvent = objectMapper.readValue<CommonPlatformHearingEvent>(commonPlatformHearing)

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
      transactionalProcessor.processMessage(person) {
        person.defendantId?.let {
          personRepository.findByDefendantId(it)
        }
      }
    }
  }

  private suspend fun getPayloadFromS3(sqsMessage: SQSMessage): String {
    val message = objectMapper.readValue(sqsMessage.message, ArrayList::class.java)

    var s3Key = (message[1] as LinkedHashMap<String, String>).get("s3Key")
    var s3Bucket = (message[1] as LinkedHashMap<String, String>).get("s3BucketName")
    val request =
      GetObjectRequest {
        key = s3Key
        bucket = s3Bucket
      }
    return s3Client.getObject(request) { resp ->
      resp.body!!.decodeToString()
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
    transactionalProcessor.processMessage(person) {
      val personEntity = searchService.searchBySourceSystem(person)
      person.defendantId = personEntity?.defendantId ?: UUID.randomUUID().toString()
      return@processMessage personEntity
    }
  }
}
