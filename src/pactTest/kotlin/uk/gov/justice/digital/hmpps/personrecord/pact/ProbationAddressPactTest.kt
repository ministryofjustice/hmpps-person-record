package uk.gov.justice.digital.hmpps.personrecord.pact

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.VerificationReports
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider
import org.apache.hc.core5.http.HttpRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.controller.probation.ProbationAddressGetAPIController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.hmpps.kotlin.auth.HmppsResourceServerConfiguration
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Minimal, standalone Spring context for the pact test: only the controller under test plus the
 * security/JWT infrastructure it needs. Explicitly excludes DataSource/JPA/Flyway/SQS
 * autoconfiguration so the context starts without Postgres or localstack.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(
  exclude = [
    DataSourceAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class,
    HibernateJpaAutoConfiguration::class,
    DataJpaRepositoriesAutoConfiguration::class,
    FlywayAutoConfiguration::class,
    uk.gov.justice.hmpps.sqs.HmppsSqsConfiguration::class,
  ],
)
@Import(
  ProbationAddressGetAPIController::class,
  HmppsResourceServerConfiguration::class,
  JwtAuthorisationHelper::class,
)
class PactTestConfiguration

/**
 * Provider-side Pact verification for the Probation Address API.
 *
 * Runs a minimal, standalone Spring context (see [PactTestConfiguration]) on a random local port
 * and verifies pacts over real HTTP via Pact's [HttpTestTarget]. The AddressRepository is mocked
 * (service boundary) so this test has no dependency on Postgres, localstack, or any other
 * container - it runs hermetically, in-process, both locally and in CI. This follows the HMPPS
 * Pact guardrail's "mock at the service boundary" provider obligation.
 *
 * (MockMvcTestTarget was tried first but pact-jvm 4.7.1's MockMvcTestTarget calls a MockMvc API
 * whose signature changed in Spring Framework 7 / Spring Boot 4, causing a NoSuchMethodError -
 * a genuine upstream incompatibility, not fixable here. HttpTestTarget avoids MockMvc entirely.)
 */
@SpringBootTest(classes = [PactTestConfiguration::class], webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Provider("hmpps-person-record")
@VerificationReports(value = ["markdown", "console"], reportDir = "build/pact")
@PactBroker
class ProbationAddressPactTest {

  @LocalServerPort
  var port: Int = 0

  @Autowired
  lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  @MockitoBean
  lateinit var addressRepository: AddressRepository

  @BeforeEach
  fun setupTarget(context: PactVerificationContext) {
    context.target = HttpTestTarget("localhost", port)
  }

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider::class)
  fun template(context: PactVerificationContext, request: HttpRequest) {
    val token = jwtAuthorisationHelper.createJwtAccessToken(roles = listOf(API_READ_ONLY))
    request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
    request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    context.verifyInteraction()
  }

  @State("An address exists for CRN and address ID")
  fun setupAddressState(): Map<String, String> {
    val crn = "X744208"
    val addressId = UUID.randomUUID()

    val personEntity = PersonEntity(
      sourceSystem = SourceSystemType.COMMON_PLATFORM,
      matchId = UUID.randomUUID(),
    ).apply { this.crn = crn }

    val addressEntity = AddressEntity(updateId = addressId, person = personEntity).apply {
      this.noFixedAbode = false
      this.startDate = ZonedDateTime.parse("2020-02-26T00:00:00Z")
      this.endDate = ZonedDateTime.parse("2023-07-15T00:00:00Z")
      this.postcode = "SW1H 9AJ"
      this.uprn = "100120991537"
      this.subBuildingName = "Sub building 2"
      this.buildingName = "Main Building"
      this.buildingNumber = "102"
      this.thoroughfareName = "Petty France"
      this.dependentLocality = "Westminster"
      this.postTown = "London"
      this.county = "Greater London"
      this.countryCode = CountryCode.GBR
      this.comment = "Some comment"
      this.statusCode = AddressStatusCode.M
      this.isVerified = true
    }

    val usageEntity = AddressUsageEntity(usageCode = AddressUsageCode.CURFEW, active = true).apply {
      this.address = addressEntity
    }
    val contactEntity = ContactEntity(contactType = ContactType.HOME, contactValue = "+44 20 7946 0000", extension = "1234").apply {
      this.address = addressEntity
    }
    addressEntity.usages = mutableListOf(usageEntity)
    addressEntity.contacts = mutableListOf(contactEntity)

    whenever(addressRepository.findByUpdateIdAndPersonCrn(addressId, crn)).thenReturn(addressEntity)

    return mapOf(
      "crn" to crn,
      "cprAddressId" to addressId.toString(),
    )
  }
}
