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
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.IEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage

const val OFFENDER_EVENTS_QUEUE_CONFIG_KEY = "cprdeliusoffendereventsqueue"

@Component
class OffenderDomainEventsListener(
  val context: ApplicationContext,
  val objectMapper: ObjectMapper,
  val featureFlag: FeatureFlag,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(OFFENDER_EVENTS_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "hmpps-person-record-cpr_delius_offender_events_queue", kind = SERVER)
  fun onDomainEvent(
    rawMessage: String,
  ) {
    if (featureFlag.isDeliusDomainEventSQSDisabled()) {
      log.debug("Domain event processing switched off.")
      return
    }

    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)

    when (sqsMessage.type) {
      "Notification" -> {
        val domainEvent = objectMapper.readValue<DomainEvent>(sqsMessage.message)
        log.debug("Received message: type:${domainEvent.eventType}")

        try {
          getEventProcessor(domainEvent).process(domainEvent)
        } catch (e: FeignException.NotFound) {
          log.info("Discarding message for status code: ${e.status()}")
        } catch (e: Exception) {
          log.error("Failed to process known domain event type:${domainEvent.eventType}", e)
          throw e
        }
      }

      else -> {
        log.info("Received a message I wasn't expecting Type: ${sqsMessage.type}")
      }
    }
  }

  fun getEventProcessor(domainEvent: DomainEvent): IEventProcessor {
    return context.getBean(domainEvent.eventType).let { it as IEventProcessor }
  }
}
