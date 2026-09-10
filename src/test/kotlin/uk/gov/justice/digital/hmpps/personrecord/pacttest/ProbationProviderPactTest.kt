package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.junitsupport.State
import org.apache.hc.core5.http.HttpRequest
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
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
  fun aProbationAddressCanBeCreatedForCrn(): Map<String, String> {
    stubNoMatchesPersonMatch()
    stubPersonMatchUpsert()

    val person = createPersonWithNewKey(
      createRandomProbationPersonDetails()
    )
    return mapOf(
      "crn" to person.crn!!,
    )
  }

  // TODO: To be updated with CPR-1328 with actual state values.
  @State("A probation person exists for CRN")
  fun aProbationPersonExistsForCrn(): Map<String, String> {
    val person = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(titleCode = TitleCode.MR),
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
    )
  }
}