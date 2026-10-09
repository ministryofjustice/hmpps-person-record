package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.ZonedDateTime

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

  private fun addressesUrl(prisonNumber: String) = "/person/prison/$prisonNumber/addresses"
}
