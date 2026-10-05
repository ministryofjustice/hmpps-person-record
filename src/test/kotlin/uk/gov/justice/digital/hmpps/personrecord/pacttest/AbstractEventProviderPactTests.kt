package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.junit5.MessageTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith

@Provider("hmpps-person-record")
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
