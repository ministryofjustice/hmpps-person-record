package uk.gov.justice.digital.hmpps.personrecord.config

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler

class RetryTestExtension : TestExecutionExceptionHandler {

  override fun handleTestExecutionException(
    context: ExtensionContext,
    throwable: Throwable,
  ) {
    context.requiredTestMethod.invoke(context.requiredTestInstance)
  }
}
