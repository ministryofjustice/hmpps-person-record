package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressUsageMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressesAndContactsResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconContactMapping
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.BUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime

class SysconSyncPrisonAddressesContactsMigrationAPIControllerIntTest : WebTestBase() {

  @Nested
  @ActiveProfiles("prod")
  inner class ProductionProfile {

    @Test
    fun `should have the correct profile active`() {
      sendPostRequestAsserted<String>(
        url = addressesUrl(randomPrisonNumber()),
        body = validRequestBody(),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
        sendAuthorised = true,
      ).returnResult().responseBody!!
    }
  }

  @Nested
  @ActiveProfiles("preprod")
  inner class PreProductionProfile {

    @Test
    fun `should have the correct profile active`() {
      sendPostRequestAsserted<String>(
        url = addressesUrl(randomPrisonNumber()),
        body = validRequestBody(),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
        sendAuthorised = true,
      ).returnResult().responseBody!!
    }
  }

  @Nested
  inner class Validation {
    // TODO
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPostRequestAsserted<String>(
        url = addressesUrl(randomPrisonNumber()),
        body = validRequestBody(),
        roles = listOf("UNSUPPORTED-ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      ).returnResult().responseBody!!
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      webTestClient.post()
        .uri(addressesUrl(randomPrisonNumber()))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class Creation {

    @Test
    fun `successful save returns the correct response body`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      val requestBody = validRequestBody()

      val response = sendPostRequestAsserted<SysconAddressesAndContactsResponseBody>(
        url = addressesUrl(prisonNumber),
        body = requestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.CREATED,
        sendAuthorised = true,
      ).returnResult().responseBody!!

      val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!

      // Addresses
      for (addressRequest in requestBody.addresses!!) {
        // Because ordering is not guaranteed, we need to find the matching address entity for each request
        val matchingAddressEntity = personEntity.addresses.single { addressMatcher(addressRequest, it) }
        assertAddressMatches(addressRequest, matchingAddressEntity)

        val addressMapping = response.addressesMappings.single { addressMatcher(addressRequest, it) }
        assertThat(addressMapping.cprAddressId).isEqualTo(matchingAddressEntity.updateId.toString())

        for (contactRequest in addressRequest.contacts) {
          // Because ordering is not guaranteed, we need to find the matching contact entity for each request
          val matchingContactEntity = matchingAddressEntity.contacts.single { contactMatcher(contactRequest, it) }
          assertContactMatches(contactRequest, matchingContactEntity)
          val contactMapping = addressMapping.contactMappings.single { contactMatcher(contactRequest, it) }
          assertThat(contactMapping.cprContactId).isEqualTo(matchingContactEntity.updateId.toString())
        }

        for (addressUsageRequest in addressRequest.addressUsage) {
          // Because ordering is not guaranteed, we need to find the matching address usage entity for each request
          val matchingAddressUsageEntity = matchingAddressEntity.usages.single { addressUsageMatcher(addressUsageRequest, it) }
          assertAddressUsageMatches(addressUsageRequest, matchingAddressUsageEntity)
          val addressUsageMapping = addressMapping.addressUsageMappings.single { addressUsageMatcher(addressUsageRequest, it) }
          assertThat(addressUsageMapping.cprAddressUsageId).isEqualTo(matchingAddressUsageEntity.updateId.toString())
        }
      }

      // Contacts
      for (contactRequest in requestBody.contacts!!) {
        // Because ordering is not guaranteed, we need to find the matching contact entity for each request
        val matchingContactEntity = personEntity.contacts.single { contactMatcher(contactRequest, it) }
        assertContactMatches(contactRequest, matchingContactEntity)
        val contactMapping = response.contactMappings.single { contactMatcher(contactRequest, it) }
        assertThat(contactMapping.cprContactId).isEqualTo(matchingContactEntity.updateId.toString())
      }
    }

    @Test
    fun `successful save deletes orphaned addresses and its children`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(
        createRandomPrisonPersonDetails(prisonNumber).copy(
          addresses = listOf(
            Address(
              fullAddress = randomFullAddress(),
              usages = listOf(AddressUsage(addressUsageCode = AddressUsageCode.A01, isActive = true)),
              contacts = listOf(
                Contact(contactType = HOME),
              ),
            ),
          ),
        ),
      )
      val person = personRepository.findByPrisonNumber(prisonNumber)!!
      val addressToBeDeleted = person.addresses[0]
      val addressUsageToBeDeleted = person.addresses[0].usages[0]
      val addressContactToBeDeleted = person.addresses[0].contacts[0]

      assertThat(addressToBeDeleted.id).isNotNull()
      assertThat(addressUsageToBeDeleted.id).isNotNull()
      assertThat(addressContactToBeDeleted.id).isNotNull()

      sendPostRequestAsserted<SysconAddressesAndContactsResponseBody>(
        url = addressesUrl(prisonNumber),
        body = validRequestBody(),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.CREATED,
        sendAuthorised = true,
      ).returnResult().responseBody!!

      // Check that we can no longer find the orphaned addresses and its children in the repository
      assertThat(addressRepository.findById(addressToBeDeleted.id!!)).isEmpty
      assertThat(contactRepository.findById(addressContactToBeDeleted.id!!)).isEmpty
      assertThat(addressUsageRepository.findById(addressUsageToBeDeleted.id!!)).isEmpty
    }
  }

  private fun addressesUrl(prisonNumber: String) = "/syscon-sync/addresses-contacts/$prisonNumber"

  private fun addressUsageMatcher(request: PrisonAddressUsage, entity: AddressUsageEntity) = entity.usageCode == request.addressUsageCode

  private fun addressUsageMatcher(request: PrisonAddressUsage, mapping: SysconAddressUsageMapping) = mapping.nomisAddressUsageId == request.nomisAddressUsageId && mapping.nomisAddressUsageCode == request.addressUsageCode

  private fun contactMatcher(request: PrisonContact, entity: ContactEntity) = entity.contactValue == request.value

  private fun contactMatcher(request: PrisonContact, mapping: SysconContactMapping) = mapping.nomisContactId == request.nomisContactId

  private fun addressMatcher(request: PrisonAddress, entity: AddressEntity) = entity.fullAddress == request.fullAddress

  private fun addressMatcher(request: PrisonAddress, mapping: SysconAddressMapping) = mapping.nomisAddressId == request.nomisAddressId

  private fun assertAddressMatches(request: PrisonAddress, address: AddressEntity) = with(address) {
    assertThat(fullAddress).isEqualTo(request.fullAddress)
    assertThat(noFixedAbode).isEqualTo(request.noFixedAbode)
    assertThat(startDate).isEqualTo(request.startDate?.toUkZonedDateTime())
    assertThat(endDate).isEqualTo(request.endDate?.toUkZonedDateTime())
    assertThat(postcode).isEqualTo(request.postcode)
    assertThat(subBuildingName).isEqualTo(request.subBuildingName)
    assertThat(buildingName).isEqualTo(request.buildingName)
    assertThat(buildingNumber).isEqualTo(request.buildingNumber)
    assertThat(thoroughfareName).isEqualTo(request.thoroughfareName)
    assertThat(dependentLocality).isEqualTo(request.dependentLocality)
    assertThat(postTown).isEqualTo(request.postTown)
    assertThat(county).isEqualTo(request.county)
    assertThat(countryCode).isEqualTo(request.countryCode)
    assertThat(comment).isEqualTo(request.comment)
    assertThat(statusCode).isEqualTo(AddressStatusCode.fromPrison(request.isPrimary, request.isMail ?: false))
  }

  private fun assertAddressUsageMatches(request: PrisonAddressUsage, entity: AddressUsageEntity) = with(entity) {
    assertThat(usageCode).isEqualTo(request.addressUsageCode)
    assertThat(active).isEqualTo(request.isActive)
  }

  private fun assertContactMatches(request: PrisonContact, entity: ContactEntity) = with(entity) {
    assertThat(contactType).isEqualTo(request.type)
    assertThat(contactValue).isEqualTo(request.value)
    assertThat(extension).isEqualTo(request.extension)
  }

  private fun validRequestBody() = PrisonAddressesAndContactsRequest(
    addresses = listOf(
      PrisonAddress(
        nomisAddressId = 10000L,
        fullAddress = "fullAddress1",
        noFixedAbode = false,
        startDate = LocalDate.of(2010, 1, 1),
        endDate = LocalDate.of(2025, 1, 1),
        postcode = "S10 3HR",
        subBuildingName = "subBuildingName1",
        buildingName = "buildingName1",
        buildingNumber = "buildingNumber1",
        thoroughfareName = "thoroughfareName1",
        dependentLocality = "dependentLocality1",
        postTown = "postTown1",
        county = "county1",
        countryCode = CountryCode.ABW,
        comment = "comment1",
        isPrimary = true,
        isMail = true,
        createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
        createUserId = "createUserIdAddress1",
        modifyDateTime = LocalDateTime.of(2020, 1, 2, 12, 0),
        modifyUserId = "modifyUserIdAddress1",
        addressUsage = listOf(
          PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.A01,
            isActive = true,
            createDateTime = LocalDateTime.of(2019, 1, 1, 12, 0),
            createUserId = "createUserIdAddress1Usage1",
            modifyDateTime = LocalDateTime.of(2019, 1, 2, 12, 0),
            modifyUserId = "modifyUserIdAddress1Usage1",
          ),
          PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.A02,
            isActive = true,
            createDateTime = LocalDateTime.of(2018, 2, 1, 12, 0),
            createUserId = "createUserIdAddress1Usage2",
            modifyDateTime = LocalDateTime.of(2018, 2, 2, 12, 0),
            modifyUserId = "modifyUserIdAddress1Usage2",
          ),
        ),
        contacts = listOf(
          PrisonContact(
            nomisContactId = 11000L,
            value = "valueAddress1Contact1",
            type = HOME,
            extension = "extensionAddress1Contact1",
            createDateTime = LocalDateTime.of(2017, 3, 1, 12, 0),
            createUserId = "createUserIdAddress1Contact1",
            modifyDateTime = LocalDateTime.of(2017, 3, 2, 12, 0),
            modifyUserId = "modifyUserIdAddress1Contact1",
          ),
          PrisonContact(
            nomisContactId = 11001L,
            value = "valueAddress1Contact2",
            type = BUS,
            extension = "extensionAddress1Contact2",
            createDateTime = LocalDateTime.of(2016, 1, 1, 12, 0),
            createUserId = "createUserIdAddress1Contact2",
            modifyDateTime = LocalDateTime.of(2016, 1, 2, 12, 0),
            modifyUserId = "modifyUserIdAddress1Contact2",
          ),
        ),
      ),
      PrisonAddress(
        nomisAddressId = 10001L,
        fullAddress = "fullAddress2",
        noFixedAbode = false,
        startDate = LocalDate.of(2010, 1, 1),
        endDate = LocalDate.of(2025, 1, 1),
        postcode = "S10 3HR",
        subBuildingName = "subBuildingName2",
        buildingName = "buildingName2",
        buildingNumber = "buildingNumber2",
        thoroughfareName = "thoroughfareName2",
        dependentLocality = "dependentLocality2",
        postTown = "postTown2",
        county = "county2",
        countryCode = CountryCode.AFG,
        comment = "comment2",
        isPrimary = false,
        isMail = false,
        createDateTime = LocalDateTime.of(2021, 1, 1, 12, 0),
        createUserId = "createUserIdAddress2",
        modifyDateTime = LocalDateTime.of(2021, 1, 2, 12, 0),
        modifyUserId = "modifyUserIdAddress2",
        addressUsage = listOf(
          PrisonAddressUsage(
            nomisAddressUsageId = 10001L,
            addressUsageCode = AddressUsageCode.A01,
            isActive = false,
            createDateTime = LocalDateTime.of(2019, 1, 1, 12, 0),
            createUserId = "createUserIdAddress2Usage1",
            modifyDateTime = LocalDateTime.of(2019, 1, 2, 12, 0),
            modifyUserId = "modifyUserIdAddress2Usage1",
          ),
        ),
        contacts = listOf(
          PrisonContact(
            nomisContactId = 11002L,
            value = "valueAddress2Contact1",
            type = HOME,
            extension = "extensionAddress2Contact1",
            createDateTime = LocalDateTime.of(2017, 7, 1, 12, 0),
            createUserId = "createUserIdAddress2Contact1",
            modifyDateTime = LocalDateTime.of(2017, 7, 2, 12, 0),
            modifyUserId = "modifyUserIdAddress2Contact1",
          ),
        ),
      ),

    ),
    contacts = listOf(
      PrisonContact(
        nomisContactId = 11003L,
        value = "valueContact1",
        type = ContactType.EMAIL,
        extension = "extensionContact1",
        createDateTime = LocalDateTime.of(2017, 4, 1, 12, 0),
        createUserId = "createUserIdContact1",
        modifyDateTime = LocalDateTime.of(2017, 4, 2, 12, 0),
        modifyUserId = "modifyUserIdContact1",
      ),
      PrisonContact(
        nomisContactId = 11004L,
        value = "valueContact2",
        type = BUS,
        extension = "extensionContact2",
        createDateTime = LocalDateTime.of(2016, 5, 1, 12, 0),
        createUserId = "createUserIdContact2",
        modifyDateTime = LocalDateTime.of(2016, 5, 2, 12, 0),
        modifyUserId = "modifyUserIdContact2",
      ),
    ),
  )
}
