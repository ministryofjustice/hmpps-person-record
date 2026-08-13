package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.core.annotation.AliasFor
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClientResponseException
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Retryable
annotation class WebRetryable(

  @get:AliasFor(annotation = Retryable::class, attribute = "maxAttemptsExpression")
  val maxAttempts: String = "3",

  @get:AliasFor(annotation = Retryable::class, attribute = "retryFor")
  val retryFor: Array<KClass<out Throwable>> = [
    WebClientResponseException.NotFound::class
  ],

  @get:AliasFor(annotation = Retryable::class, attribute = "backoff")
  val backoff: Backoff = Backoff(delay = 500, maxDelay = 1000),
)
