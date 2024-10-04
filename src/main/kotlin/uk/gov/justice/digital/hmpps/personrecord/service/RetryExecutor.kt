package uk.gov.justice.digital.hmpps.personrecord.service

import feign.FeignException
import kotlinx.coroutines.delay
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.orm.jpa.JpaSystemException
import kotlin.reflect.KClass

object RetryExecutor {
  private val retryables = listOf(feign.RetryableException::class, FeignException.InternalServerError::class, FeignException.ServiceUnavailable::class, FeignException.BadGateway::class)
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

  val ENTITY_RETRY_EXCEPTIONS = listOf(
    ObjectOptimisticLockingFailureException::class,
    CannotAcquireLockException::class,
    JpaSystemException::class,
    JpaObjectRetrievalFailureException::class,
    DataIntegrityViolationException::class,
    ConstraintViolationException::class,
  )
}
