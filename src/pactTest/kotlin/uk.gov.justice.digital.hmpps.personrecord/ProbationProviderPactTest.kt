package uk.gov.justice.digital.hmpps.personrecord

import au.com.dius.pact.provider.junitsupport.State
import org.apache.hc.core5.http.HttpRequest
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
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
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.M
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode.GBR
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.PRIMARY
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class ProbationProviderPactTest : AbstractProviderPactTests() {
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

  override fun rolesFor(request: HttpRequest): List<String> = when (request.method.uppercase()) {
    "POST" -> listOf(PROBATION_API_READ_WRITE)
    else -> listOf(API_READ_ONLY)
  }

  @State("An address exists for CRN and address ID")
  fun anAddressExistsForCrnAndAddressId() {
    whenever(addressRepository.findByUpdateIdAndPersonCrn(any(), any())).thenAnswer { invocation ->
      buildAddressEntity(
        crn = invocation.arguments[1] as String,
        cprAddressId = invocation.arguments[0] as UUID,
      )
    }
  }

  @State("A probation address can be created for CRN")
  fun aProbationAddressCanBeCreatedForCrn() {
    whenever(addressService.processAddress(any(), any(), any(), eq(CPR))).thenReturn(
      AddressEntity(
        updateId = createdAddressId,
        statusCode = M,
        isVerified = true,
      ),
    )
  }

  @State("A probation person exists for CRN")
  fun aProbationPersonExistsForCrn() {
    whenever(personRepository.findByCrn(any())).thenAnswer { invocation ->
      buildPersonEntity(invocation.arguments[0] as String)
    }
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
