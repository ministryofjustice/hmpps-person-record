package uk.gov.justice.digital.hmpps.personrecord.service

import feign.FeignException
import kotlinx.coroutines.delay
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

object RetryExecutor {
  private const val JITTER_MIN = 0.8
  private const val JITTER_MAX = 1.2
  private const val EXPONENTIAL_FACTOR = 2.0

  private val retryables = listOf(feign.RetryableException::class, FeignException.InternalServerError::class, FeignException.ServiceUnavailable::class, FeignException.BadGateway::class)

  suspend fun <T> runWithRetry(
    maxAttempts: Int,
    delay: Long,
    exceptions: List<KClass<out Exception>> = retryables,
    maxDelayMillis: Long = 1000,
    action: suspend () -> T,
  ): T {
    val log = LoggerFactory.getLogger(this::class.java)
    var currentDelay = delay
    var lastException: Exception? = null

    repeat(maxAttempts) {
      try {
        return action()
      } catch (e: Exception) {
        when {
          e::class in exceptions -> {
            lastException = e

            log.debug("Retrying on Raised Error: ${e.message}")
            val jitterValue = Random.nextDouble(JITTER_MIN, JITTER_MAX)
            val delayTime = (currentDelay * jitterValue).toLong()

            delay(delayTime)

            currentDelay = min((currentDelay * EXPONENTIAL_FACTOR).toLong(), maxDelayMillis)
          }
          else -> throw e
        }
      }
    }
    throw lastException ?: RuntimeException("Unexpected error")
  }

  val ENTITY_RETRY_EXCEPTIONS = listOf(
    ObjectOptimisticLockingFailureException::class,
    CannotAcquireLockException::class,
    JpaSystemException::class,
    JpaObjectRetrievalFailureException::class,
    DataIntegrityViolationException::class,
    ConstraintViolationException::class,
  )
}
