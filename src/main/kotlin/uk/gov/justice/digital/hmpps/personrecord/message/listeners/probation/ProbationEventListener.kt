package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import feign.FeignException
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.MESSAGE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.TimeoutExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.PROBATION_EVENT_QUEUE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED

@Component
@Profile("!seeding")
class ProbationEventListener(
  private val eventProcessor: ProbationEventProcessor,
  private val objectMapper: ObjectMapper,
  private val telemetryService: TelemetryService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = TimeoutExecutor.runWithTimeout {
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
    when (sqsMessage.type) {
      NOTIFICATION -> {
        when (sqsMessage.messageAttributes?.eventType?.value) {
          OFFENDER_ALIAS_CHANGED -> handleAliasUpdate(sqsMessage)
          else -> handleDomainEvent(sqsMessage)
        }
      }
      else -> log.info("Received a message I wasn't expecting Type: ${sqsMessage.type}")
    }
  }

  private fun handleDomainEvent(sqsMessage: SQSMessage) {
    val domainEvent = objectMapper.readValue<DomainEvent>(sqsMessage.message)
    val crn = domainEvent.personReference?.identifiers?.first { it.type == "CRN" }!!.value
    processEvent(crn, domainEvent.eventType, sqsMessage.messageId)
  }

  private fun handleAliasUpdate(sqsMessage: SQSMessage) {
    val probationEvent = objectMapper.readValue<ProbationEvent>(sqsMessage.message)
    val eventType = sqsMessage.messageAttributes?.eventType?.value!!
    processEvent(probationEvent.crn, eventType, sqsMessage.messageId)
  }

  private fun processEvent(crn: String, eventType: String, messageId: String?) {
    try {
      eventProcessor.processEvent(crn, eventType)
    } catch (e: FeignException.NotFound) {
      log.info("Discarding message for status code: ${e.status()}")
    } catch (e: Exception) {
      telemetryService.trackEvent(
        MESSAGE_PROCESSING_FAILED,
        mapOf(
          EVENT_TYPE to eventType,
          SOURCE_SYSTEM to DELIUS.name,
          MESSAGE_ID to messageId,
        ),
      )
      throw e
    }
  }
}
