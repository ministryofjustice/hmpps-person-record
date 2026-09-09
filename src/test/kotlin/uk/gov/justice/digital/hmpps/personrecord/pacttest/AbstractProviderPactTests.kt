package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import org.apache.hc.core5.http.HttpRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.web.server.LocalServerPort
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

/**
 * Base class for Pact provider verification tests.
 *
 * Wires up the provider name, broker connection and pact source, along with the shared
 * Spring context and authentication needed to run the verification. Endpoint-specific
 * test classes should extend this and only need to provide `@State` methods and, where
 * required, override [rolesFor] to reflect the roles their endpoints expect.
 */
@Provider("hmpps-person-record")
//@PactFolder("src/test/resources/pacts")
@PactBroker(url = $$"${pactbroker.url}")
abstract class AbstractProviderPactTests: WebTestBase() {
  @LocalServerPort
  private var port: Int = 0

  @BeforeEach
  fun setUpPactVerification(context: PactVerificationContext) {
    context.target = HttpTestTarget("localhost", port)
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider::class)
  fun pactVerificationTestTemplate(context: PactVerificationContext, request: HttpRequest) {
    val token = jwtAuthorisationHelper.createJwtAccessToken(roles = rolesFor(request))
    request.setHeader("Authorization", "Bearer $token")
    request.setHeader("Content-Type", "application/json")
    context.verifyInteraction()
  }

  /**
   * Determines the roles the JWT auth token should carry for a given interaction request.
   * Override in subclasses whose endpoints require different roles per request.
   */
  protected open fun rolesFor(request: HttpRequest): List<String> = listOf(API_READ_ONLY)
}
