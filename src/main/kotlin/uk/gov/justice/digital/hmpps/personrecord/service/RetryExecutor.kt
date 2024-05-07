package uk.gov.justice.digital.hmpps.personrecord.service

import feign.FeignException
import kotlinx.coroutines.delay
import kotlin.reflect.KClass

object RetryExecutor {
  private val retryables = listOf(feign.RetryableException::class, FeignException.InternalServerError::class, FeignException.ServiceUnavailable::class)
  suspend fun <T> runWithRetry(
    maxAttempts: Int,
    delay: Long,
    exceptions: List<KClass<out Exception>> = retryables,
    retryFunction: suspend () -> T,
  ): T {
    var lastException: Exception? = null
    repeat(maxAttempts) {
      try {
        return retryFunction()
      } catch (e: Exception) {
        if (e::class in exceptions) {
          lastException = e
        } else {
          throw e
        }
      }
      delay(delay)
    }
    throw lastException ?: RuntimeException("Unexpected error")
  }
}
