package uk.gov.justice.digital.hmpps.personrecord.service.queue

import jakarta.persistence.OptimisticLockException
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.CprRetryable
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.service.TimeoutExecutor

@Component
class SQSListenerService(
  private val jsonMapper: JsonMapper,
) {

  @CprRetryable(
    retryFor = [
      OptimisticLockException::class,
      DataIntegrityViolationException::class,
      CannotAcquireLockException::class,
    ],
  )
  fun processSQSMessage(rawMessage: String, action: (sqsMessage: SQSMessage) -> Unit) = TimeoutExecutor.runWithTimeout {
    val sqsMessage = jsonMapper.readValue<SQSMessage>(rawMessage)
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
  }
}

class DiscardableNotFoundException : RuntimeException()

fun <T : Any> Mono<out T>.discardNotFoundException(): Mono<out T> = this.onErrorResume(WebClientResponseException::class.java) {
  if (it.statusCode == NOT_FOUND) {
    throw DiscardableNotFoundException()
  }
  Mono.error(it)
}
