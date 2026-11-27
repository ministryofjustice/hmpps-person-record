package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.junitsupport.State
import org.apache.hc.core5.http.HttpRequest
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ASN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.OTHR
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonsNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomContactType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligionCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import uk.gov.justice.digital.hmpps.personrecord.test.randomZonedDateTime

class ApiProviderPactTest : AbstractApiProviderPactTests() {
  override fun rolesFor(request: HttpRequest): List<String> = when (request.method.uppercase()) {
    "POST" -> listOf(PROBATION_API_READ_WRITE, API_READ_ONLY)
    else -> listOf(API_READ_ONLY)
  }

  @State("An address exists for the requested CRN and address ID")
  fun anAddressExistsForCrnAndAddressId(): Map<String, String> {
    val crn = randomCrn()
    val person = createProbationPersonWithAddress(crn)

    return mapOf(
      "crn" to crn,
      "cprAddressId" to person.addresses.first().updateId!!.toString(),
    )
  }

  @State("A probation address can be created for the requested CRN")
  fun aProbationAddressCanBeCreatedForCrn(): Map<String, String> {
    val person = createPersonWithNewKey(
      createRandomProbationPersonDetails(randomCrn()),
    )
    return mapOf("crn" to person.crn!!)
  }

  @State("A probation person exists for the requested CRN")
  fun aProbationPersonExistsForCrn(): Map<String, String> {
    val person = createProbationPersonWithAddress(randomCrn())
    return mapOf("crn" to person.crn!!)
  }

  @State("A person exists for the requested common platform Id")
  fun aPersonExistsForTheRequestedCommonPlatformId(): Map<String, String> = createCourtDataIngestionPerson()

  @State("A person exists for the requested prisoner number")
  fun aPersonExistsForTheRequestedPrisonerNumber(): Map<String, String> = createCourtDataIngestionPerson()

  @State("A person exists as an exact match for the requested first name, last name, date of birth and PNC")
  fun aPersonExistsForExactMatch(): Map<String, String> {
    val person = createRandomPrisonPersonDetails()
    createPersonKey()
      .addPerson(person)
      .addPerson(person.copy(prisonNumber = null, crn = randomCrn(), sourceSystem = DELIUS))
      .addPerson(person.copy(prisonNumber = null, defendantId = randomDefendantId(), sourceSystem = COMMON_PLATFORM))
      .addPerson(person.copy(prisonNumber = null, cId = randomCId(), sourceSystem = LIBRA))

    return mapOf(
      "firstName" to person.firstName!!,
      "lastName" to person.lastName!!,
      "dateOfBirth" to person.dateOfBirth.toString(),
      "pnc" to person.getPnc().toString(),
    )
  }

  private fun createProbationPersonWithAddress(crn: String) = createPersonWithNewKey(
    createRandomProbationPersonDetails(crn),
    configure = addAddressToRecord(buildPactAddress()),
  )

  private fun createCourtDataIngestionPerson(): Map<String, String> {
    val defendantId = randomDefendantId()
    val prisonNumber = randomPrisonNumber()
    val prisonPerson = createRandomPrisonPersonDetails(prisonNumber)
    val title = randomTitleCode().value
    val sex = randomPrisonSexCode().value

    val personKey = createPersonKey()
      .addPerson(createRandomCommonPlatformPersonDetails(defendantId))
      .addPerson(createRandomProbationPersonDetails(randomCrn()))
      .addPerson(createRandomLibraPersonDetails())
    createPerson(
      prisonPerson.copy(
        middleNames = randomName(),
        titleCode = title,
        sexCode = sex,
        ethnicityCode = EthnicityCode.fromPrison(randomPrisonEthnicity()),
        sexualOrientation = randomPrisonSexualOrientation().value,
        disability = randomBoolean(),
        immigrationStatus = randomBoolean(),
        aliases = listOf(Alias(firstName = randomName(), middleNames = randomName(), lastName = randomName(), dateOfBirth = randomDate(), titleCode = title, sexCode = sex)),
        addresses = listOf(buildPactAddress()),
        contacts = listOf(buildPactContact()),
        references = prisonPerson.references + listOf(
          Reference(identifierType = NATIONAL_INSURANCE_NUMBER, identifierValue = randomNationalInsuranceNumber()),
          Reference(identifierType = ASN, identifierValue = randomArrestSummonsNumber()),
          Reference(identifierType = DRIVER_LICENSE_NUMBER, identifierValue = randomDriverLicenseNumber()),
          Reference(identifierType = OTHR, identifierValue = randomName()),
        ),
      ),
      personKey,
    ) { religion = randomReligionCode() }

    return mapOf(
      "defendantId" to defendantId,
      "prisonerNumber" to prisonNumber,
    )
  }

  private fun buildPactContact(): Contact = Contact(
    contactType = randomContactType(),
    contactValue = randomPhoneNumber(),
    extension = "+44",
    createDateTime = randomDateTime(),
    createUserId = randomName(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
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
    usages = listOf(AddressUsage(randomAddressUsageCode(), randomBoolean(), randomDateTime(), randomName(), randomDateTime(), randomName())),
    contacts = listOf(buildPactContact()),
    createDateTime = randomDateTime(),
    createUserId = randomName(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
  )
}
