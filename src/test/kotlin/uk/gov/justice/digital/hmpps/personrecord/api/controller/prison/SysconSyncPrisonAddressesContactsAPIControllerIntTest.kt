package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime

class SysconSyncPrisonAddressesContactsAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class Validation {

    @Test
    fun `should respond with 501 as not currently implemented`() {
      webTestClient.post()
        .uri(addressesUrl(randomPrisonNumber()))
        .bodyValue(PrisonAddressesAndContactsRequest(addresses = emptyList(), contacts = null))
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isEqualTo(NOT_IMPLEMENTED)
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.post()
        .uri(addressesUrl(randomPrisonNumber()))
        .bodyValue(
          PrisonAddressesAndContactsRequest(
            addresses = listOf(
              PrisonAddress(
                nomisAddressId = 10000L,
                fullAddress = "102 Petty France, London",
                startDate = LocalDate.of(2020, 1, 1),
                postcode = "SW1H 9AJ",
                countryCode = CountryCode.GBR,
                isPrimary = true,
                createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
                createUserId = "billybob",
              ),
            ),
            contacts = listOf(
              PrisonContact(
                nomisContactId = 10000L,
                value = "01234567890",
                type = HOME,
                createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
                createUserId = "johnnydoe",
              ),
            ),
          ),
        )
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
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

  private fun addressesUrl(prisonNumber: String) = "/syscon-sync/addresses-contacts/$prisonNumber"
}
