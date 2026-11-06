package uk.gov.justice.digital.hmpps.personrecord.config

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import uk.gov.justice.digital.hmpps.personrecord.pacttest.AbstractProviderPactTests

class RetryTestExtension : TestExecutionExceptionHandler {

  override fun handleTestExecutionException(
    context: ExtensionContext,
    throwable: Throwable,
  ) {
    if (context.requiredTestInstance is AbstractProviderPactTests) {
      throw throwable
    }

    println("retrying once: ${throwable.message}")
    context.requiredTestMethod.invoke(context.requiredTestInstance)
  }
}
