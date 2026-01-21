package uk.gov.justice.digital.hmpps.personrecord

import org.springframework.core.annotation.AliasFor
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Retryable
annotation class CprRetryable(

  @get:AliasFor(annotation = Retryable::class, attribute = "maxAttemptsExpression")
  val maxAttempts: String = "5",

  @get:AliasFor(annotation = Retryable::class, attribute = "retryFor")
  val retryFor: Array<KClass<out Throwable>> = [Exception::class],

  @get:AliasFor(annotation = Retryable::class, attribute = "backoff")
  val backoff: Backoff = Backoff(delay = 200, random = true, multiplier = 3.0),
)
