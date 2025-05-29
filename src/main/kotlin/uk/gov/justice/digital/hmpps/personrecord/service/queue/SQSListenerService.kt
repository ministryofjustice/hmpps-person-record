package uk.gov.justice.digital.hmpps.personrecord.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.TimeoutExecutor

@Component
class SQSListenerService(
  private val objectMapper: ObjectMapper,
) {

  fun processDomainEvent(rawMessage: String, action: (domainEvent: DomainEvent) -> Unit) = processSQSMessage(rawMessage) { action(objectMapper.readValue<DomainEvent>(it.message)) }

  fun processSQSMessage(rawMessage: String, action: (sqsMessage: SQSMessage) -> Unit) = TimeoutExecutor.runWithTimeout {
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
    when (sqsMessage.type) {
      NOTIFICATION -> action(sqsMessage)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun DomainEvent.discardWhenNotFoundException(action: (domainEvent: DomainEvent) -> Unit): DomainEvent {
      try {
        action(this)
      } catch (e: WebClientResponseException.NotFound) {
        log.info("Discarding message for status code: ${e.statusCode} ${e.request?.uri}")
      }
      return this
    }

    fun DomainEvent.whenEvent(eventType: String, action: (domainEvent: DomainEvent) -> Unit): DomainEvent {
      when {
        this.eventType == eventType -> action(this)
      }
      return this
    }
  }
}
