package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
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
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(OFFENDER_EVENTS_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(
    rawMessage: String,
  ) {
    log.debug("Enter onDomainEvent")
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
    featureFlag.isDeliusDomainEventSQSEnabled().let {
      when (sqsMessage.type) {
        "Notification" -> {
          val domainEvent = objectMapper.readValue<DomainEvent>(sqsMessage.message)
          log.debug("Received message: type:${domainEvent.eventType}")

          try {
            getEventProcessor(domainEvent).process(domainEvent)
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
  }

  fun getEventProcessor(domainEvent: DomainEvent): IEventProcessor {
    return context.getBean(domainEvent.eventType).let { it as IEventProcessor }
  }
}
