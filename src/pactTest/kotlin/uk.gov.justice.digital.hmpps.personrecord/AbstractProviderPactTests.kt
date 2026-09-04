package uk.gov.justice.digital.hmpps.personrecord

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import org.apache.hc.core5.http.HttpRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

/**
 * Base class for Pact provider verification tests.
 *
 * Wires up the provider name, broker connection and pact source, along with the shared
 * Spring context and authentication needed to run the verification. Endpoint-specific
 * test classes should extend this and only need to provide `@State` methods and, where
 * required, override [rolesFor] to reflect the roles their endpoints expect.
 */
@ActiveProfiles("test")
@Provider("hmpps-person-record")
@PactFolder("src/pactTest/resources/pacts")
@PactBroker(url = "\${pactbroker.url}")
@SpringBootTest(
  classes = [PactTestConfiguration::class],
  webEnvironment = RANDOM_PORT,
)
abstract class AbstractProviderPactTests {

  @LocalServerPort
  private var port: Int = 0

  @Autowired
  lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

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
