package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import feign.FeignException
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis.PrisonerEventsProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.model.types.SQSMessageType

const val PRISONER_EVENTS_QUEUE_CONFIG_KEY = "cprnomiseventsqueue"

@Component
class PrisonerDomainEventsListener(
  val context: ApplicationContext,
  val objectMapper: ObjectMapper,
  val prisonerEventsProcessor: PrisonerEventsProcessor,
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
      SQSMessageType.NOTIFICATION -> {
        val domainEvent = objectMapper.readValue<DomainEvent>(sqsMessage.message)
        handleEvent(domainEvent)
      }
      else -> log.info("Received a message I wasn't expecting Type: ${sqsMessage.type}")
    }
  }

  fun handleEvent(domainEvent: DomainEvent) {
    try {
      prisonerEventsProcessor.processEvent(domainEvent)
    } catch (e: FeignException.NotFound) {
      log.info("Discarding message for status code: ${e.status()}")
    } catch (e: Exception) {
      log.error("Failed to process known domain event type:${domainEvent.eventType}", e)
      throw e
    }
  }
}
