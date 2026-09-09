package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.junitsupport.State
import org.apache.hc.core5.http.HttpRequest
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.M
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode.A02
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode.GBR
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.W1
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode.F
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation.HET
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode.MR
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode.MS
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.BRIT
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.justice.digital.hmpps.personrecord.test.*

@Autowired
private lateinit var jdbcTemplate: JdbcTemplate

class ProbationProviderPactTest : AbstractProviderPactTests() {
  override fun rolesFor(request: HttpRequest): List<String> = when (request.method.uppercase()) {
    "POST" -> listOf(PROBATION_API_READ_WRITE)
    else -> listOf(API_READ_ONLY)
  }

  // TODO: To be updated with CPR-1328 with actual state values.
  @State("An address exists for CRN and address ID")
  fun anAddressExistsForCrnAndAddressId(): Map<String, String> {
    deleteAllPersonData()
    val person = createPersonWithNewKey(
      createRandomProbationPersonDetails(),
      configure = addAddressToRecord(
        Address(
          noFixedAbode = randomBoolean(),
          startDate = randomZonedDateTime(),
          endDate = randomZonedDateTime(),
          postcode = randomPostcode(),
          buildingName = randomName(),
          subBuildingName = randomName(),
          buildingNumber = randomBuildingNumber(),
          thoroughfareName = randomName(),
          dependentLocality = randomName(),
          postTown = randomName(),
          county = randomName(),
          countryCode = randomCountryCode(),
          uprn = randomUprn(),
          statusCode = randomAddressStatusCode(),
          comment = randomName(),
          isVerified = randomBoolean(),
          usages = listOf(AddressUsage(randomAddressUsageCode(), randomBoolean())),
          contacts = listOf(Contact(randomContactType(), randomPhoneNumber(), "+44")),
        ),
      ),
    )

    return mapOf(
      "crn" to person.crn!!,
      "cprAddressId" to person.addresses.first().updateId.toString(),
    )
  }

  // TODO: To be updated with CPR-1328 with actual state values.
  @State("A probation address can be created for CRN")
  fun aProbationAddressCanBeCreatedForCrn() {
//    deleteAllPersonData()
//    createPersonWithNewKey(buildPactPerson("X12345").copy(addresses = emptyList()))
  }

  // TODO: To be updated with CPR-1328 with actual state values.
  @State("A probation person exists for CRN")
  fun aProbationPersonExistsForCrn() {
//    deleteAllPersonData()
//    createProbationPersonWithAddress("X12345")
  }
}


//  private fun buildPactPerson(crn: String): Person = Person(
//    firstName = "Jane",
//    middleNames = "Ann",
//    lastName = "Doe",
//    dateOfBirth = LocalDate.of(1990, 8, 21),
//    crn = crn,
//    prisonNumber = "A1234BC",
//    titleCode = MR,
//    sexCode = F,
//    ethnicityCode = W1,
//    sexualOrientation = HET,
//    religion = AGNO,
//    disability = false,
//    immigrationStatus = false,
//    aliases = listOf(
//      Alias(
//        firstName = "Jane",
//        middleNames = "Ann",
//        lastName = "Doe",
//        dateOfBirth = LocalDate.of(1990, 8, 21),
//        titleCode = MS,
//        sexCode = F,
//      ),
//    ),
//    nationalities = listOf(BRIT),
//    references = listOf(
//      Reference(identifierType = CRO, identifierValue = "123456/00A"),
//      Reference(identifierType = PNC, identifierValue = "2000/1234567A"),
//    ),
//    addresses = listOf(buildPactAddress()),
//    sourceSystem = DELIUS,
//  )

//  private fun buildPactAddress(): Address = Address(
//      noFixedAbode = false,
//      startDate = ZonedDateTime.of(2026, 5, 15, 12, 8, 46, 347_000_000, ZoneOffset.UTC),
//      endDate = ZonedDateTime.of(2026, 7, 15, 12, 8, 46, 347_000_000, ZoneOffset.UTC),
//      postcode = "SW1H 9AJ",
//      subBuildingName = "Sub building 2",
//      buildingName = "Main Building",
//      buildingNumber = "102",
//      thoroughfareName = "Petty France",
//      dependentLocality = "Westminster",
//      postTown = "London",
//      county = "Greater London",
//      countryCode = GBR,
//      uprn = "100120991537",
//      comment = "Address created from probation",
//      statusCode = M,
//      isVerified = true,
//      usages = listOf(AddressUsage(addressUsageCode = A02, isActive = true)),
//      contacts = listOf(Contact(contactType = HOME, contactValue = "+44 20 7946 0000", extension = "1234")),
//    )
//  }
