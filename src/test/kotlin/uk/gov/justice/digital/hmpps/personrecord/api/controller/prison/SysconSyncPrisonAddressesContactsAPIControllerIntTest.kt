package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.http.HttpStatus.UNAUTHORIZED
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
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonAddressesContactsAPIControllerIntTest : WebTestBase() {

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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        sendDeleteRequestAsserted<SysconAddressMapping>(
          url = deletePrisonerAddressUrl(prisonNumber, addressId),
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
        sendDeleteRequestAsserted<String>(
          url = deletePrisonerAddressUrl(prisonNumber, addressId),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        sendDeleteRequestAsserted<SysconAddressMapping>(
          url = deletePrisonerAddressUrl(prisonNumber, addressId),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  inner class CreatePrisonerContact {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendPostRequestAsserted<SysconContactMapping>(
          url = createPrisonerContactUrl(randomPrisonNumber()),
          body = PrisonContact(
            nomisContactId = 10000L,
            value = "01234567890",
            type = HOME,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "johnnydoe",
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
          url = createPrisonerContactUrl(randomPrisonNumber()),
          body = PrisonContact(
            nomisContactId = 10000L,
            value = "01234567890",
            type = HOME,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "johnnydoe",
          ),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<SysconContactMapping>(
          url = createPrisonerContactUrl(randomPrisonNumber()),
          body = PrisonContact(
            nomisContactId = 10000L,
            value = "01234567890",
            type = HOME,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "johnnydoe",
          ),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  inner class CreatePrisonerAddressContact {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendPostRequestAsserted<SysconContactMapping>(
          url = createPrisonerAddressContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = PrisonContact(
            nomisContactId = 10000L,
            value = "01234567890",
            type = HOME,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "johnnydoe",
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
          url = createPrisonerAddressContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = PrisonContact(
            nomisContactId = 10000L,
            value = "01234567890",
            type = HOME,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "johnnydoe",
          ),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<SysconContactMapping>(
          url = createPrisonerAddressContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = PrisonContact(
            nomisContactId = 10000L,
            value = "01234567890",
            type = HOME,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "johnnydoe",
          ),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  inner class UpdatePrisonerContact {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        val prisonNumber = randomPrisonNumber()
        val contactId = UUID.randomUUID().toString()
        sendPutRequestAsserted<SysconContactMapping>(
          url = updatePrisonerContactUrl(prisonNumber, contactId),
          body = PrisonContact(
            nomisContactId = 10000L,
            value = "01234567890",
            type = HOME,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "johnnydoe",
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
        val contactId = UUID.randomUUID().toString()
        sendPutRequestAsserted<String>(
          url = updatePrisonerContactUrl(prisonNumber, contactId),
          body = PrisonContact(
            nomisContactId = 10000L,
            value = "01234567890",
            type = HOME,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "johnnydoe",
          ),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        val prisonNumber = randomPrisonNumber()
        val contactId = UUID.randomUUID().toString()
        sendPutRequestAsserted<SysconContactMapping>(
          url = updatePrisonerContactUrl(prisonNumber, contactId),
          body = PrisonContact(
            nomisContactId = 10000L,
            value = "01234567890",
            type = HOME,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "johnnydoe",
          ),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  inner class DeletePrisonerContact {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        val prisonNumber = randomPrisonNumber()
        val contactId = UUID.randomUUID().toString()
        sendDeleteRequestAsserted<SysconContactMapping>(
          url = deletePrisonerContactUrl(prisonNumber, contactId),
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
        val contactId = UUID.randomUUID().toString()
        sendDeleteRequestAsserted<String>(
          url = deletePrisonerContactUrl(prisonNumber, contactId),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        val prisonNumber = randomPrisonNumber()
        val contactId = UUID.randomUUID().toString()
        sendDeleteRequestAsserted<SysconContactMapping>(
          url = deletePrisonerContactUrl(prisonNumber, contactId),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  inner class CreatePrisonerAddressUsage {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendPostRequestAsserted<SysconAddressUsageMapping>(
          url = createPrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
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
          url = createPrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<SysconAddressUsageMapping>(
          url = createPrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
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
  inner class UpdatePrisonerAddressUsage {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        val usageId = UUID.randomUUID().toString()
        sendPutRequestAsserted<SysconAddressUsageMapping>(
          url = updatePrisonerAddressUsageUrl(prisonNumber, addressId, usageId),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
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
        val usageId = UUID.randomUUID().toString()
        sendPutRequestAsserted<String>(
          url = updatePrisonerAddressUsageUrl(prisonNumber, addressId, usageId),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
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
        val usageId = UUID.randomUUID().toString()
        sendPutRequestAsserted<SysconContactMapping>(
          url = updatePrisonerAddressUsageUrl(prisonNumber, addressId, usageId),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
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
  inner class DeletePrisonerAddressUsage {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        val usageId = UUID.randomUUID().toString()
        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerAddressUsageUrl(prisonNumber, addressId, usageId),
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
        val usageId = UUID.randomUUID().toString()
        sendDeleteRequestAsserted<String>(
          url = deletePrisonerAddressUsageUrl(prisonNumber, addressId, usageId),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        val usageId = UUID.randomUUID().toString()
        sendDeleteRequestAsserted<SysconContactMapping>(
          url = deletePrisonerAddressUsageUrl(prisonNumber, addressId, usageId),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  private fun addressesUrl(prisonNumber: String) = "/syscon-sync/addresses-contacts/$prisonNumber"
  private fun createPrisonerAddressUrl(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/address"
  private fun updatePrisonerAddressUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId"
  private fun deletePrisonerAddressUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId"
  private fun createPrisonerContactUrl(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/contact"
  private fun createPrisonerAddressContactUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId/contact"
  private fun updatePrisonerContactUrl(prisonNumber: String, contactId: String) = "/syscon-sync/person/$prisonNumber/contact/$contactId"
  private fun deletePrisonerContactUrl(prisonNumber: String, contactId: String) = "/syscon-sync/person/$prisonNumber/contact/$contactId"
  private fun createPrisonerAddressUsageUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId/usage"
  private fun updatePrisonerAddressUsageUrl(prisonNumber: String, addressId: String, usageId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId/usage/$usageId"
  private fun deletePrisonerAddressUsageUrl(prisonNumber: String, addressId: String, usageId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId/usage/$usageId"
}
