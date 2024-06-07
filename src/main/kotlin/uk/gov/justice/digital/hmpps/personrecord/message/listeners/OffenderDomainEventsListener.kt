package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import feign.FeignException
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.message.processors.delius.OffenderEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED

const val OFFENDER_EVENTS_QUEUE_CONFIG_KEY = "cprdeliusoffendereventsqueue"

@Component
@Profile("!seeding")
class OffenderDomainEventsListener(
  val eventProcessor: OffenderEventProcessor,
  val objectMapper: ObjectMapper,
  val telemetryService: TelemetryService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(OFFENDER_EVENTS_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "hmpps-person-record-cpr_delius_offender_events_queue", kind = SERVER)
  fun onDomainEvent(
    rawMessage: String,
  ) {
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

  fun handleEvent(domainEvent: DomainEvent, messageId: String?) {
    try {
      eventProcessor.processEvent(domainEvent)
    } catch (e: FeignException.NotFound) {
      log.info("Discarding message for status code: ${e.status()}")
    } catch (e: Exception) {
      telemetryService.trackEvent(
        MESSAGE_PROCESSING_FAILED,
        mapOf(
          EventKeys.EVENT_TYPE to domainEvent.eventType,
          EventKeys.SOURCE_SYSTEM to SourceSystemType.DELIUS.name,
          EventKeys.MESSAGE_ID to messageId,
        ),
      )
      throw e
    }
  }
}
