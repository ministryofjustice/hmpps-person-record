package uk.gov.justice.digital.hmpps.personrecord.service

import feign.FeignException
import kotlinx.coroutines.delay
import org.hibernate.StaleObjectStateException
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.orm.jpa.JpaSystemException
import kotlin.math.min
import kotlin.random.Random
import kotlin.reflect.KClass

private const val DB_TRY_COUNT = 5
private const val HTTP_TRY_COUNT = 3

private const val EXPONENTIAL_FACTOR = 2.0
private const val JITTER_MIN = 0.8
private const val JITTER_MAX = 1.2
private const val MAX_DELAY_MILLIS: Long = 1000

object RetryExecutor {

  suspend fun <T> runWithRetryHTTP(
    delay: Long,
    action: suspend () -> T,
  ): T = runWithRetry(HTTP_TRY_COUNT, delay, HTTP_RETRY_EXCEPTIONS, action)

  suspend fun <T> runWithRetryDatabase(
    delay: Long,
    action: suspend () -> T,
  ): T = runWithRetry(DB_TRY_COUNT, delay, ENTITY_RETRY_EXCEPTIONS, action)
  private suspend fun <T> runWithRetry(
    maxAttempts: Int,
    delay: Long,
    exceptions: List<KClass<out Exception>>,
    action: suspend () -> T,
  ): T {
    var currentDelay = delay
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

  private val ENTITY_RETRY_EXCEPTIONS = listOf(
    ObjectOptimisticLockingFailureException::class,
    CannotAcquireLockException::class,
    JpaSystemException::class,
    JpaObjectRetrievalFailureException::class,
    DataIntegrityViolationException::class,
    ConstraintViolationException::class,
    StaleObjectStateException::class,
  )

  private val HTTP_RETRY_EXCEPTIONS = listOf(feign.RetryableException::class, FeignException.InternalServerError::class, FeignException.ServiceUnavailable::class, FeignException.BadGateway::class)
}
