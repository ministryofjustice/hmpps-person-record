package uk.gov.justice.digital.hmpps.personrecord

import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.junit5.HttpTestTarget
import org.apache.hc.core5.http.HttpRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.probation.ProbationOverrideHandler
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.M
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode.GBR
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.PRIMARY
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

@Provider("hmpps-person-record")
@PactBroker
@SpringBootTest(
  classes = [PactTestConfiguration::class],
  webEnvironment = RANDOM_PORT
)
class ProbationProviderPactTest {
  private val createdAddressId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

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
    val token = jwtAuthorisationHelper.createJwtAccessToken(roles = rolesFor(request))
    request.setHeader("Authorization", "Bearer $token")
    request.setHeader("Content-Type", "application/json")
    context.verifyInteraction()
  }

  @State("An address exists for CRN and address ID")
  fun `an address exists for CRN and address ID`() {
    Mockito.doAnswer { invocation ->
      buildAddressEntity(
        crn = invocation.arguments[1] as String,
        cprAddressId = invocation.arguments[0] as UUID,
      )
    }.`when`(addressRepository).findByUpdateIdAndPersonCrn(
      ArgumentMatchers.any(UUID::class.java),
      ArgumentMatchers.anyString(),
    )
  }

  @State("A probation address can be created for CRN")
  fun `a probation address can be created for CRN`() {
    Mockito.doReturn(
      AddressEntity(
        updateId = createdAddressId,
        statusCode = M,
        isVerified = true,
      ),
    ).`when`(addressService).processAddress(
      ArgumentMatchers.any() as Address,
      ArgumentMatchers.any() as () -> PersonEntity?,
      ArgumentMatchers.any() as () -> AddressEntity?,
      ArgumentMatchers.eq(uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR),
    )
  }

  @State("A probation person exists for CRN")
  fun `a probation person exists for CRN`() {
    Mockito.doAnswer { invocation ->
      buildPersonEntity(invocation.arguments[0] as String)
    }.`when`(personRepository).findByCrn(ArgumentMatchers.anyString())
  }

  private fun rolesFor(request: HttpRequest): List<String> = when (request.method.uppercase()) {
    "POST" -> listOf(PROBATION_API_READ_WRITE)
    else -> listOf(API_READ_ONLY)
  }

  private fun buildPersonEntity(crn: String): PersonEntity {
    val personKey = PersonKeyEntity.new()
    val person = PersonEntity.new(DELIUS).apply {
      this.crn = crn
      this.religion = AGNO
      this.lastModified = LocalDateTime.of(2026, 1, 1, 0, 0)
      this.pseudonyms.add(
        PseudonymEntity(
          firstName = "Jane",
          lastName = "Doe",
          dateOfBirth = LocalDate.of(1990, 1, 1),
          sexCode = SexCode.F,
          nameType = PRIMARY,
        ).also { it.person = this },
      )
      this.references.add(
        ReferenceEntity(identifierType = CRO, identifierValue = "123456/00A").also { it.person = this },
      )
      this.references.add(
        ReferenceEntity(identifierType = PNC, identifierValue = "2000/1234567A").also { it.person = this },
      )
    }
    person.assignToPersonKey(personKey)
    person.addresses.add(
      buildAddressEntity(
        crn = crn,
        cprAddressId = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        person = person,
      ),
    )
    return person
  }

  private fun buildAddressEntity(
    crn: String,
    cprAddressId: UUID,
    person: PersonEntity = PersonEntity.new(DELIUS).apply { this.crn = crn },
  ): AddressEntity = AddressEntity(
    updateId = cprAddressId,
    person = person,
    startDate = ZonedDateTime.of(2026, 1, 1, 9, 30, 0, 0, ZoneOffset.UTC),
    noFixedAbode = false,
    postcode = "SW1H 9AJ",
    buildingName = "Main Building",
    buildingNumber = "102",
    thoroughfareName = "Petty France",
    postTown = "London",
    county = "Greater London",
    countryCode = GBR,
    comment = "Primary address",
    statusCode = M,
    isVerified = true,
  )
}
