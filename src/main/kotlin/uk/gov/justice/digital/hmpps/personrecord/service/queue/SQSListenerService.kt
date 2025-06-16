package uk.gov.justice.digital.hmpps.personrecord.service.queue

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.OptimisticLockException
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent

@Component
class SQSListenerService(
  private val objectMapper: ObjectMapper,
) {

  fun processDomainEvent(rawMessage: String, action: (domainEvent: DomainEvent) -> Unit) = processSQSMessage(rawMessage) { action(objectMapper.readValue<DomainEvent>(it.message)) }

  @Retryable(
    maxAttempts = 5,
    backoff = Backoff(delay = 200, random = true, multiplier = 3.0),
    retryFor = [
      OptimisticLockException::class,
      DataIntegrityViolationException::class,
      CannotAcquireLockException::class,
    ],
  )

  fun processSQSMessage(rawMessage: String, action: (sqsMessage: SQSMessage) -> Unit) = TimeoutExecutor.runWithTimeout {
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
    try {
      when (sqsMessage.type) {
        NOTIFICATION -> action(sqsMessage)
      }
    } catch (_: DiscardableNotFoundException) {
      log.info("Discarding message for status code")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun DomainEvent.whenEvent(eventType: String, action: (domainEvent: DomainEvent) -> Unit): DomainEvent {
      when {
        this.eventType == eventType -> action(this)
      }
      return this
    }
  }
}

class DiscardableNotFoundException : RuntimeException()

fun <T> Mono<out T>.discardNotFoundException(): Mono<out T> = this.onErrorResume(WebClientResponseException::class.java) {
  if (it.statusCode == NOT_FOUND) {
    throw DiscardableNotFoundException()
  } else {
    Mono.error(it)
  }
}
