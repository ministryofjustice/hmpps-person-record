package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException.BadGateway
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError
import org.springframework.web.reactive.function.client.WebClientResponseException.ServiceUnavailable
import kotlin.math.min
import kotlin.random.Random
import kotlin.reflect.KClass

private const val HTTP_TRY_COUNT = 3

private const val EXPONENTIAL_FACTOR = 2.0
private const val JITTER_MIN = 0.8
private const val JITTER_MAX = 1.2
private const val MAX_DELAY_MILLIS: Long = 1000

@Component
class RetryExecutor(@Value("\${retry.delay}") val delayMillis: Long) {

  suspend fun <T> runWithRetryHTTP(
    action: suspend () -> T,
  ): T = runWithRetry(HTTP_TRY_COUNT, httpRetryExceptions, action)

  private suspend fun <T> runWithRetry(
    maxAttempts: Int,
    exceptions: List<KClass<out Exception>>,
    action: suspend () -> T,
  ): T {
    var currentDelay = delayMillis
    var lastException: Exception? = null
    val log = LoggerFactory.getLogger(this::class.java)

    repeat(maxAttempts) {
      try {
        return action()
      } catch (e: Exception) {
        when {
          e::class in exceptions -> {
            lastException = e
            log.info("Retrying on error: ${e.message}")

            val jitterValue = Random.nextDouble(JITTER_MIN, JITTER_MAX)
            val delayTime = (currentDelay * jitterValue).toLong()

            delay(delayTime)

            currentDelay = min((currentDelay * EXPONENTIAL_FACTOR).toLong(), MAX_DELAY_MILLIS)
          }
          else -> {
            log.info("Failed to retry on class ${e::class}")
            throw e
          }
        }
      }
    }
    throw lastException ?: RuntimeException("Unexpected error")
  }

  private val httpRetryExceptions = listOf(InternalServerError::class, BadGateway::class, ServiceUnavailable::class)
}
