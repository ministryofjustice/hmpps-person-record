package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.junit5.MessageTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Base class for Pact async message provider verification tests.
 *
 * Concrete verifiers must provide their own `@Provider` annotation with the
 * appropriate logical provider name (e.g., "hmpps-person-record" for prison events,
 * or "hmpps-person-record-events" for SAS events).
 */
abstract class AbstractEventProviderPactTests {
  private val pactProviderScanPackages = listOf("uk.gov.justice.digital.hmpps.personrecord.pacttest")

  @BeforeEach
  fun setUpPactVerification(context: PactVerificationContext) {
    context.target = MessageTestTarget(pactProviderScanPackages, javaClass.classLoader)
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider::class)
  fun testTemplate(context: PactVerificationContext) {
    context.verifyInteraction()
  }
}
