package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.delay
import kotlin.reflect.KClass

private const val DELAY: Long = 2000

object RetryExecutor {
  suspend fun <T> runWithRetry(
    exceptions: List<KClass<out Exception>>,
    maxAttempts: Int,
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
      delay(DELAY)
    }
    throw lastException ?: RuntimeException("Unexpected error")
  }
}
