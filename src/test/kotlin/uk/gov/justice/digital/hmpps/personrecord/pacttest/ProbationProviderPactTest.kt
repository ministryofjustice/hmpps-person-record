package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.junitsupport.State
import org.apache.hc.core5.http.HttpRequest
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomContactType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import uk.gov.justice.digital.hmpps.personrecord.test.randomZonedDateTime

class ProbationProviderPactTest : AbstractProviderPactTests() {
  override fun rolesFor(request: HttpRequest): List<String> = when (request.method.uppercase()) {
    "POST" -> listOf(PROBATION_API_READ_WRITE)
    else -> listOf(API_READ_ONLY)
  }

  @State("An address exists for CRN and address ID")
  fun anAddressExistsForCrnAndAddressId(): Map<String, String> {
    val crn = REQUESTED_CRN
    deleteAllPersonData()

    val person = createProbationPersonWithAddress(crn)
    val persistedAddress = addressRepository.findById(person.addresses.first().id!!).orElseThrow()

    return mapOf(
      "crn" to crn,
      "cprAddressId" to persistedAddress.updateId.toString(),
    )
  }

  @State("A probation address can be created for CRN")
  fun aProbationAddressCanBeCreatedForCrn(): Map<String, String> {
    deleteAllPersonData()

    stubNoMatchesPersonMatch()
    stubPersonMatchUpsert()

    val person = createProbationPerson(REQUESTED_CRN)
    return mapOf("crn" to person.crn!!)
  }

  @State("A probation person exists for CRN")
  fun aProbationPersonExistsForCrn(): Map<String, String> {
    deleteAllPersonData()

    val person = createProbationPersonWithAddress(REQUESTED_CRN)
    return mapOf("crn" to person.crn!!)
  }

  private fun createProbationPerson(crn: String) = createPersonWithNewKey(
    buildPactPerson(crn).copy(addresses = emptyList()),
  )

  private fun createProbationPersonWithAddress(crn: String) = createPersonWithNewKey(
    buildPactPerson(crn),
    configure = addAddressToRecord(buildPactAddress()),
  )

  private fun buildPactPerson(crn: String): Person = createRandomProbationPersonDetails(crn).copy(
    titleCode = TitleCode.MR,
  )

  private fun buildPactAddress(): Address = Address(
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
  )

  private companion object {
    const val REQUESTED_CRN = "X12345"
  }
}
