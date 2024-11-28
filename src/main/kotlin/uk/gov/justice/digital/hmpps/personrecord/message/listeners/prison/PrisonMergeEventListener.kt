package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonMergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.MESSAGE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.TimeoutExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED

const val PRISON_MERGE_EVENT_QUEUE_CONFIG_KEY = "cprnomismergeeventsqueue"

@Component
@Profile("!seeding")
class PrisonMergeEventListener(
  val mergeEventProcessor: PrisonMergeEventProcessor,
  val objectMapper: ObjectMapper,
  val telemetryService: TelemetryService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(PRISON_MERGE_EVENT_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(
    rawMessage: String,
  ) {
    TimeoutExecutor.runWithTimeout {
      val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
      when (sqsMessage.type) {
        NOTIFICATION -> {
          val domainEvent = objectMapper.readValue<DomainEvent>(sqsMessage.message)
          handleEvent(domainEvent, sqsMessage.messageId)
        }

        else -> {
          log.info("Received a message I wasn't expecting Type: ${sqsMessage.type}")
        }
      }
    }
  }

  private fun handleEvent(domainEvent: DomainEvent, messageId: String?) {
    try {
      mergeEventProcessor.processEvent(domainEvent)
    } catch (e: Exception) {
      telemetryService.trackEvent(
        MESSAGE_PROCESSING_FAILED,
        mapOf(
          EVENT_TYPE to domainEvent.eventType,
          SOURCE_SYSTEM to SourceSystemType.NOMIS.name,
          MESSAGE_ID to messageId,
        ),
      )
      throw e
    }
  }
}
