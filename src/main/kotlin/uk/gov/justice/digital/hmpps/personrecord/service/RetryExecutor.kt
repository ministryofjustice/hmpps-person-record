package uk.gov.justice.digital.hmpps.personrecord.service

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import java.time.Duration
import java.util.function.Supplier

class RetryExecutor<T> {
  companion object{
    const val MAX_ATTEMPTS = 3
  }
  private val retryConfig: RetryConfig = RetryConfig.custom<T>()
    .maxAttempts(MAX_ATTEMPTS)
    .waitDuration(Duration.ofMillis(2000))
    .build()

  fun executeWithRetry(supplier: Supplier<T>): T? {
    return try {
      Retry.decorateSupplier(Retry.of("id", retryConfig), supplier).get()
    } catch (e: Exception) {
      throw RuntimeException("Failed after $MAX_ATTEMPTS retry attempts", e)
    }
  }
}
