package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import feign.FeignException
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis.PrisonerEventsProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED

const val PRISONER_EVENTS_QUEUE_CONFIG_KEY = "cprnomiseventsqueue"

@Component
class PrisonerDomainEventsListener(
  val objectMapper: ObjectMapper,
  val prisonerEventsProcessor: PrisonerEventsProcessor,
  val telemetryService: TelemetryService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(PRISONER_EVENTS_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "hmpps-person-record-cpr_nomis_events_queue", kind = SERVER)
  fun onDomainEvent(
    rawMessage: String,
  ) {
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
    when (sqsMessage.type) {
      NOTIFICATION -> {
        val domainEvent = objectMapper.readValue<DomainEvent>(sqsMessage.message)
        handleEvent(domainEvent, sqsMessage.messageId)
      }
      else -> log.info("Received a message I wasn't expecting Type: ${sqsMessage.type}")
    }
  }

  fun handleEvent(domainEvent: DomainEvent, messageId: String?) {
    try {
      prisonerEventsProcessor.processEvent(domainEvent)
    } catch (e: FeignException.NotFound) {
      log.info("Discarding message for status code: ${e.status()}")
    } catch (e: Exception) {
      telemetryService.trackEvent(
        MESSAGE_PROCESSING_FAILED,
        mapOf(
          EventKeys.EVENT_TYPE to domainEvent.eventType,
          EventKeys.SOURCE_SYSTEM to SourceSystemType.NOMIS.name,
          EventKeys.MESSAGE_ID to messageId,
        ),
      )
      throw e
    }
  }
}
