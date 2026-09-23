package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.*
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressMapping
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonAddressesAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class CreatePrisonerAddress {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        val prisonNumber = randomPrisonNumber()
        sendPostRequestAsserted<SysconAddressMapping>(
          url = createPrisonerAddressUrl(prisonNumber),
          body = PrisonAddress(
            nomisAddressId = 10000L,
            fullAddress = "102 Petty France, London",
            startDate = LocalDate.of(2020, 1, 1),
            postcode = "SW1H 9AJ",
            countryCode = CountryCode.GBR,
            isPrimary = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
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
          url = createPrisonerAddressUrl(randomPrisonNumber()),
          body = PrisonAddress(
            nomisAddressId = 10000L,
            fullAddress = "102 Petty France, London",
            startDate = LocalDate.of(2020, 1, 1),
            postcode = "SW1H 9AJ",
            countryCode = CountryCode.GBR,
            isPrimary = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<SysconAddressMapping>(
          url = createPrisonerAddressUrl(randomPrisonNumber()),
          body = PrisonAddress(
            nomisAddressId = 10000L,
            fullAddress = "102 Petty France, London",
            startDate = LocalDate.of(2020, 1, 1),
            postcode = "SW1H 9AJ",
            countryCode = CountryCode.GBR,
            isPrimary = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  inner class UpdatePrisonerAddress {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        sendPutRequestAsserted<SysconAddressMapping>(
          url = updatePrisonerAddressUrl(prisonNumber, addressId),
          body = PrisonAddress(
            nomisAddressId = 10000L,
            fullAddress = "102 Petty France, London",
            startDate = LocalDate.of(2020, 1, 1),
            postcode = "SW1H 9AJ",
            countryCode = CountryCode.GBR,
            isPrimary = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        sendPutRequestAsserted<String>(
          url = updatePrisonerAddressUrl(prisonNumber, addressId),
          body = PrisonAddress(
            nomisAddressId = 10000L,
            fullAddress = "102 Petty France, London",
            startDate = LocalDate.of(2020, 1, 1),
            postcode = "SW1H 9AJ",
            countryCode = CountryCode.GBR,
            isPrimary = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        sendPutRequestAsserted<SysconAddressMapping>(
          url = updatePrisonerAddressUrl(prisonNumber, addressId),
          body = PrisonAddress(
            nomisAddressId = 10000L,
            fullAddress = "102 Petty France, London",
            startDate = LocalDate.of(2020, 1, 1),
            postcode = "SW1H 9AJ",
            countryCode = CountryCode.GBR,
            isPrimary = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  inner class DeletePrisonerAddress {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerAddressUrl(randomPrisonNumber(),  UUID.randomUUID().toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendDeleteRequestAsserted<String>(
          url = deletePrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  private fun createPrisonerAddressUrl(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/address"
  private fun updatePrisonerAddressUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId"
  private fun deletePrisonerAddressUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId"
}
