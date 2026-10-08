package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonAddressRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactRequest
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.M
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.MA
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.P
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.PM
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.BUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit.MICROS
import java.time.temporal.ChronoUnit.MINUTES
import java.util.UUID

class PrisonAPIAddressesControllerIntTest : WebTestBase() {

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /person/prison/{prisonNumber}/addresses")
  inner class GetAddressesByPrisonNumber {

    private val prisonNumber = randomPrisonNumber()

    @BeforeAll
    fun beforeAll() {
      createPersonWithNewKey(
        createRandomPrisonPersonDetails(prisonNumber).copy(
          addresses = listOf(
            Address(postcode = "AB1 1AA", startDate = ZonedDateTime.parse("2010-01-01T00:00:00Z")),
            Address(postcode = "CD2 2BB", startDate = null),
            Address(postcode = "EF3 3CC", startDate = ZonedDateTime.parse("2020-01-01T00:00:00Z"), buildingName = "Building"),
          ),
        ),
      )
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should return all addresses for the prisoner, most recent first`() {
        val addresses = sendGetRequestAsserted<List<CanonicalAddress>>(
          url = addressesUrl(prisonNumber),
          roles = listOf(API_READ_ONLY),
          expectedStatus = OK,
        ).returnResult().responseBody!!

        assertThat(addresses.map { it.postcode }).containsExactly("EF3 3CC", "AB1 1AA", "CD2 2BB")
        assertThat(addresses.first().buildingName).isEqualTo("Building")
        assertThat(addresses).allSatisfy { assertThat(it.cprAddressId).isNotBlank() }
      }

      @Test
      fun `should return an empty list when the prisoner has no addresses`() {
        val prisonNumberWithoutAddresses = randomPrisonNumber()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumberWithoutAddresses).copy(addresses = emptyList()))

        val addresses = sendGetRequestAsserted<List<CanonicalAddress>>(
          url = addressesUrl(prisonNumberWithoutAddresses),
          roles = listOf(API_READ_ONLY),
          expectedStatus = OK,
        ).returnResult().responseBody!!

        assertThat(addresses).isEmpty()
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when the prison number does not exist`() {
        val unknownPrisonNumber = randomPrisonNumber()
        val response = sendGetRequestAsserted<String>(
          url = addressesUrl(unknownPrisonNumber),
          roles = listOf(API_READ_ONLY),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("Person with $unknownPrisonNumber not found")
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendGetRequestAsserted<String>(
          url = addressesUrl(prisonNumber),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendGetRequestAsserted<Unit>(
          url = addressesUrl(prisonNumber),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("POST /person/prison/{prisonNumber}/addresses")
  inner class CreateAddressByPrisonNumber {

    @Nested
    inner class HappyPath {

      @BeforeEach
      fun beforeEach() {
        stubPersonMatchUpsert()
        stubPersonMatchScores()
      }

      @Test
      fun `should create the address and return it`() {
        val prisonNumber = randomPrisonNumber()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber).copy(addresses = emptyList()))

        val created = sendPostRequestAsserted<CanonicalAddress>(
          url = addressesUrl(prisonNumber),
          body = validAddressRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = CREATED,
        ).returnResult().responseBody!!

        assertThat(created.cprAddressId).isNotBlank()
        assertThat(created.subBuildingName).isEqualTo("3B")
        assertThat(created.buildingNumber).isEqualTo("102")
        assertThat(created.thoroughfareName).isEqualTo("Slinn Street")
        assertThat(created.dependentLocality).isEqualTo("Brincliffe")
        assertThat(created.postTown).isEqualTo("Liverpool")
        assertThat(created.county).isEqualTo("Merseyside")
        assertThat(created.countryCode).isEqualTo(CountryCode.ENG.name)
        assertThat(created.postcode).isEqualTo("LI1 5TH")
        assertThat(created.status.code).isEqualTo(M.name)
        assertThat(created.noFixedAbode).isFalse()
        assertThat(created.startDate).isEqualTo("2020-01-01")
        assertThat(created.endDate).isNull()
        assertThat(created.usages.map { it.usageCode.code }).containsExactly(AddressUsageCode.CURFEW.name)
        assertThat(created.usages).allSatisfy {
          assertThat(it.isActive).isTrue()
          assertThat(it.createUserId).isEqualTo(USER_ID)
          assertThat(it.createDateTime).isCloseTo(LocalDateTime.now(), within(1, MINUTES))
          assertThat(it.modifyUserId).isNull()
          assertThat(it.modifyDateTime).isNull()
        }
        assertThat(created.createUserId).isEqualTo(USER_ID)
        assertThat(created.createDateTime).isCloseTo(LocalDateTime.now(), within(1, MINUTES))
        assertThat(created.modifyUserId).isNull()
        assertThat(created.modifyDateTime).isNull()

        val addresses = sendGetRequestAsserted<List<CanonicalAddress>>(
          url = addressesUrl(prisonNumber),
          roles = listOf(API_READ_ONLY),
          expectedStatus = OK,
        ).returnResult().responseBody!!
        // Postgres rounds to microseconds whereas the response is built before persisting, so may have nanosecond precision
        val withinOneMicro = Comparator<LocalDateTime> { a, b ->
          if (Duration.between(a, b).abs() <= Duration.of(1, MICROS)) 0 else a.compareTo(b)
        }
        assertThat(addresses).singleElement()
          .usingRecursiveComparison()
          .withComparatorForType(nullsFirst(withinOneMicro), LocalDateTime::class.java)
          .isEqualTo(created)
      }

      @Test
      fun `should remove the primary flag from existing addresses when new address is primary`() {
        val statuses = createAddressAndGetExistingStatuses(M)

        assertThat(statuses).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            PM_POSTCODE to (MA to USER_ID),
            M_POSTCODE to (null to USER_ID),
            MA_POSTCODE to (MA to null),
            P_POSTCODE to (P to null),
          ),
        )
      }

      @Test
      fun `should remove the mail flag from existing addresses when new address is mail`() {
        val statuses = createAddressAndGetExistingStatuses(MA)

        assertThat(statuses).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            PM_POSTCODE to (M to USER_ID),
            M_POSTCODE to (M to null),
            MA_POSTCODE to (null to USER_ID),
            P_POSTCODE to (P to null),
          ),
        )
      }

      @Test
      fun `should remove both flags from existing addresses when new address is primary and mail`() {
        val statuses = createAddressAndGetExistingStatuses(PM)

        assertThat(statuses).containsExactlyInAnyOrderEntriesOf(
          mapOf(
            PM_POSTCODE to (null to USER_ID),
            M_POSTCODE to (null to USER_ID),
            MA_POSTCODE to (null to USER_ID),
            P_POSTCODE to (P to null),
          ),
        )
      }

      /**
       * Returns existing address postcode to (status code, modify user id) after creating a new address
       */
      private fun createAddressAndGetExistingStatuses(newStatusCode: AddressStatusCode): Map<String?, Pair<AddressStatusCode?, String?>> {
        val prisonNumber = randomPrisonNumber()
        createPersonWithNewKey(
          createRandomPrisonPersonDetails(prisonNumber).copy(
            addresses = listOf(
              Address(postcode = PM_POSTCODE, statusCode = PM),
              Address(postcode = M_POSTCODE, statusCode = M),
              Address(postcode = MA_POSTCODE, statusCode = MA),
              Address(postcode = P_POSTCODE, statusCode = P),
            ),
          ),
        )

        val created = sendPostRequestAsserted<CanonicalAddress>(
          url = addressesUrl(prisonNumber),
          body = validAddressRequest().copy(statusCode = newStatusCode),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = CREATED,
        ).returnResult().responseBody!!
        assertThat(created.status.code).isEqualTo(newStatusCode.name)

        return personRepository.findByPrisonNumber(prisonNumber)!!.addresses
          .filter { it.updateId.toString() != created.cprAddressId }
          .associate { it.postcode to (it.statusCode to it.modifyUserId) }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when the prison number does not exist`() {
        val unknownPrisonNumber = randomPrisonNumber()
        val response = sendPostRequestAsserted<String>(
          url = addressesUrl(unknownPrisonNumber),
          body = validAddressRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("Person with $unknownPrisonNumber not found")
      }

      @Test
      fun `should return bad request when start date is missing`() {
        sendPostRequestAsserted<String>(
          url = addressesUrl(randomPrisonNumber()),
          body = mapOf(
            "countryCode" to CountryCode.ENG.name,
            "statusCode" to M.name,
            "usages" to listOf(AddressUsageCode.CURFEW.name),
            "userId" to "TEST",
          ),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }

      @Test
      fun `should return bad request when status code is invalid`() {
        sendPostRequestAsserted<String>(
          url = addressesUrl(randomPrisonNumber()),
          body = mapOf(
            "countryCode" to CountryCode.ENG.name,
            "statusCode" to "NOT_A_STATUS",
            "startDate" to "2020-01-01",
            "usages" to listOf(AddressUsageCode.CURFEW.name),
            "userId" to "TEST",
          ),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPostRequestAsserted<String>(
          url = addressesUrl(randomPrisonNumber()),
          body = validAddressRequest(),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<Unit>(
          url = addressesUrl(randomPrisonNumber()),
          body = validAddressRequest(),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }

    private fun validAddressRequest() = PrisonAddressRequest(
      subBuildingName = "3B",
      buildingNumber = "102",
      thoroughfareName = "Slinn Street",
      dependentLocality = "Brincliffe",
      postTown = "Liverpool",
      county = "Merseyside",
      countryCode = CountryCode.ENG,
      postcode = "LI1 5TH",
      statusCode = M,
      noFixedAbode = false,
      startDate = LocalDate.of(2020, 1, 1),
      usages = listOf(AddressUsageCode.CURFEW),
      userId = USER_ID,
    )
  }

  @Nested
  @DisplayName("POST /person/prison/{prisonNumber}/addresses/{addressId}/phone-numbers")
  inner class CreateAddressPhoneNumbersByPrisonNumber {

    @Nested
    inner class HappyPath {
      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendPostRequestAsserted<Unit>(
          url = addressPhoneNumbersUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = listOf(validPhoneNumberRequest()),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return bad request when value is missing`() {
        sendPostRequestAsserted<String>(
          url = addressPhoneNumbersUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = listOf(mapOf("type" to BUS.name, "userId" to "TEST")),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }

      @Test
      fun `should return bad request when contact type is invalid`() {
        sendPostRequestAsserted<String>(
          url = addressPhoneNumbersUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = listOf(mapOf("type" to "NOT_A_TYPE", "value" to "01234 567 890", "userId" to "TEST")),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPostRequestAsserted<String>(
          url = addressPhoneNumbersUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = listOf(validPhoneNumberRequest()),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<Unit>(
          url = addressPhoneNumbersUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = listOf(validPhoneNumberRequest()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }

    private fun validPhoneNumberRequest() = PrisonContactRequest(
      type = BUS,
      value = "01234 567 890",
      extension = "123",
      userId = "TEST",
    )
  }

  private companion object {
    const val USER_ID = "CREATE_USER"
    const val PM_POSTCODE = "PM1 1AA"
    const val M_POSTCODE = "M1 1AA"
    const val MA_POSTCODE = "MA1 1AA"
    const val P_POSTCODE = "P1 1AA"
  }

  private fun addressesUrl(prisonNumber: String) = "/person/prison/$prisonNumber/addresses"
  private fun addressPhoneNumbersUrl(prisonNumber: String, addressId: String) = "/person/prison/$prisonNumber/addresses/$addressId/phone-numbers"
}
