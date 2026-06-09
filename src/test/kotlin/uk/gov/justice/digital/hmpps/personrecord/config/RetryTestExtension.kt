package uk.gov.justice.digital.hmpps.personrecord.config

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler

class RetryTestExtension : TestExecutionExceptionHandler {

  override fun handleTestExecutionException(
    context: ExtensionContext,
    throwable: Throwable,
  ) {
    println("retrying once: ${throwable.message}")
    context.requiredTestMethod.invoke(context.requiredTestInstance)
  }
}
