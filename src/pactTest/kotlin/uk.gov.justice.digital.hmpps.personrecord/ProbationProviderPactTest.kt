package uk.gov.justice.digital.hmpps.personrecord

import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.junit5.HttpTestTarget
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.personrecord.api.handler.probation.ProbationOverrideHandler
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import org.apache.hc.core5.http.HttpRequest
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY

@Provider("hmpps-person-record")
@PactBroker
@SpringBootTest(
  classes = [PactTestConfiguration::class],
  webEnvironment = RANDOM_PORT
)
class ProbationProviderPactTest {
  @MockitoBean
  lateinit var addressRepository: AddressRepository

  @MockitoBean
  lateinit var addressService: AddressService

  @MockitoBean
  lateinit var personRepository: PersonRepository

  @MockitoBean
  lateinit var personService: PersonService

  @MockitoBean
  lateinit var probationOverrideHandler: ProbationOverrideHandler

  @LocalServerPort
  private var port: Int = 0

  @Autowired
  lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  @BeforeEach
  fun setUp(context: PactVerificationContext) {
    context.target = HttpTestTarget("localhost", port)
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider::class)
  fun pactVerificationTestTemplate(context: PactVerificationContext, request: HttpRequest) {
    val token = jwtAuthorisationHelper.createJwtAccessToken(roles = listOf(API_READ_ONLY))
    request.setHeader("Authorization", "Bearer $token")
    request.setHeader("Content-Type", "application/json")
    context.verifyInteraction()
  }

//  @State("XXXXXX")
//  fun `XXXXXX`() {
//    val mockXXXX = XXXX(
//      XXXX = XXXX
//    )
//    whenever(XXXXXX.).thenReturn(mockXXXX)
//  }
}
