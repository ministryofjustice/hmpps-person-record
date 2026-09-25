package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressesAndContactsResponseBody
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime

class SysconSyncPrisonAddressesContactsMigrationAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class SaveAddressesAndContacts {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendPostRequestAsserted<Unit>(
          url = addressesUrl(randomPrisonNumber()),
          body = PrisonAddressesAndContactsRequest(addresses = emptyList(), contacts = null),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPostRequestAsserted<String>(
          url = addressesUrl(randomPrisonNumber()),
          body = PrisonAddressesAndContactsRequest(
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
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<SysconAddressesAndContactsResponseBody>(
          url = addressesUrl(randomPrisonNumber()),
          body = PrisonAddressesAndContactsRequest(addresses = emptyList(), contacts = null),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  private fun addressesUrl(prisonNumber: String) = "/syscon-sync/addresses-contacts/$prisonNumber"
}
