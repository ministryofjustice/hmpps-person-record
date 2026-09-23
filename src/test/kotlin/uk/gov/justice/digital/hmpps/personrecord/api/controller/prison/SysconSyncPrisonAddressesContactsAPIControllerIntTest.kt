package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
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
  }

  @Nested
  inner class CreatePrisonerAddress {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        val prisonNumber = randomPrisonNumber()
        webTestClient.post()
          .uri(createPrisonerAddressUrl(prisonNumber))
          .bodyValue(
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
          )
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
        val prisonNumber = randomPrisonNumber()
        webTestClient.post()
          .uri(createPrisonerAddressUrl(prisonNumber))
          .bodyValue(
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
        val prisonNumber = randomPrisonNumber()
        webTestClient.post()
          .uri(createPrisonerAddressUrl(prisonNumber))
          .exchange()
          .expectStatus()
          .isUnauthorized
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
        webTestClient.put()
          .uri(updatePrisonerAddressUrl(prisonNumber, addressId))
          .bodyValue(
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
          )
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        webTestClient.put()
          .uri(updatePrisonerAddressUrl(prisonNumber, addressId))
          .bodyValue(
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        webTestClient.put()
          .uri(updatePrisonerAddressUrl(prisonNumber, addressId))
          .exchange()
          .expectStatus()
          .isUnauthorized
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
        webTestClient.delete()
          .uri(deletePrisonerAddressUrl(prisonNumber, addressId))
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        webTestClient.delete()
          .uri(deletePrisonerAddressUrl(prisonNumber, addressId))
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        webTestClient.delete()
          .uri(deletePrisonerAddressUrl(prisonNumber, addressId))
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  @Nested
  inner class CreatePrisonerContact {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        val prisonNumber = randomPrisonNumber()
        webTestClient.post()
          .uri(createPrisonerContactUrl(prisonNumber))
          .bodyValue(
            PrisonContact(
              nomisContactId = 10000L,
              value = "01234567890",
              type = HOME,
              createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
              createUserId = "johnnydoe",
            ),
          )
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
        val prisonNumber = randomPrisonNumber()
        webTestClient.post()
          .uri(createPrisonerContactUrl(prisonNumber))
          .bodyValue(
            PrisonContact(
              nomisContactId = 10000L,
              value = "01234567890",
              type = HOME,
              createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
              createUserId = "johnnydoe",
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
        val prisonNumber = randomPrisonNumber()
        webTestClient.post()
          .uri(createPrisonerContactUrl(prisonNumber))
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  @Nested
  inner class CreatePrisonerAddressContact {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        webTestClient.post()
          .uri(createPrisonerAddressContactUrl(prisonNumber, addressId))
          .bodyValue(
            PrisonContact(
              nomisContactId = 10000L,
              value = "01234567890",
              type = HOME,
              createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
              createUserId = "johnnydoe",
            ),
          )
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        webTestClient.post()
          .uri(createPrisonerAddressContactUrl(prisonNumber, addressId))
          .bodyValue(
            PrisonContact(
              nomisContactId = 10000L,
              value = "01234567890",
              type = HOME,
              createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
              createUserId = "johnnydoe",
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        webTestClient.post()
          .uri(createPrisonerAddressContactUrl(prisonNumber, addressId))
          .exchange()
          .expectStatus()
          .isUnauthorized
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
        webTestClient.put()
          .uri(updatePrisonerContactUrl(prisonNumber, contactId))
          .bodyValue(
            PrisonContact(
              nomisContactId = 10000L,
              value = "01234567890",
              type = HOME,
              createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
              createUserId = "johnnydoe",
            ),
          )
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
        val prisonNumber = randomPrisonNumber()
        val contactId = UUID.randomUUID().toString()
        webTestClient.put()
          .uri(updatePrisonerContactUrl(prisonNumber, contactId))
          .bodyValue(
            PrisonContact(
              nomisContactId = 10000L,
              value = "01234567890",
              type = HOME,
              createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
              createUserId = "johnnydoe",
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
        val prisonNumber = randomPrisonNumber()
        val contactId = UUID.randomUUID().toString()
        webTestClient.put()
          .uri(updatePrisonerContactUrl(prisonNumber, contactId))
          .exchange()
          .expectStatus()
          .isUnauthorized
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
        webTestClient.delete()
          .uri(deletePrisonerContactUrl(prisonNumber, contactId))
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
        val prisonNumber = randomPrisonNumber()
        val contactId = UUID.randomUUID().toString()
        webTestClient.delete()
          .uri(deletePrisonerContactUrl(prisonNumber, contactId))
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
        val prisonNumber = randomPrisonNumber()
        val contactId = UUID.randomUUID().toString()
        webTestClient.delete()
          .uri(deletePrisonerContactUrl(prisonNumber, contactId))
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  @Nested
  inner class CreatePrisonerAddressUsage {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        webTestClient.post()
          .uri(createPrisonerAddressUsageUrl(prisonNumber, addressId))
          .bodyValue(
            PrisonAddressUsage(
              nomisAddressUsageId = 10000L,
              addressUsageCode = AddressUsageCode.CURFEW,
              isActive = true,
              createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
              createUserId = "billybob",
            ),
          )
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        webTestClient.post()
          .uri(createPrisonerAddressUsageUrl(prisonNumber, addressId))
          .bodyValue(
            PrisonAddressUsage(
              nomisAddressUsageId = 10000L,
              addressUsageCode = AddressUsageCode.CURFEW,
              isActive = true,
              createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
              createUserId = "billybob",
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        webTestClient.post()
          .uri(createPrisonerAddressUsageUrl(prisonNumber, addressId))
          .exchange()
          .expectStatus()
          .isUnauthorized
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
        webTestClient.put()
          .uri(updatePrisonerAddressUsageUrl(prisonNumber, addressId, usageId))
          .bodyValue(
            PrisonAddressUsage(
              nomisAddressUsageId = 10000L,
              addressUsageCode = AddressUsageCode.CURFEW,
              isActive = true,
              createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
              createUserId = "billybob",
            ),
          )
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        val usageId = UUID.randomUUID().toString()
        webTestClient.put()
          .uri(updatePrisonerAddressUsageUrl(prisonNumber, addressId, usageId))
          .bodyValue(
            PrisonAddressUsage(
              nomisAddressUsageId = 10000L,
              addressUsageCode = AddressUsageCode.CURFEW,
              isActive = true,
              createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
              createUserId = "billybob",
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        val usageId = UUID.randomUUID().toString()
        webTestClient.put()
          .uri(updatePrisonerAddressUsageUrl(prisonNumber, addressId, usageId))
          .exchange()
          .expectStatus()
          .isUnauthorized
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
        webTestClient.delete()
          .uri(deletePrisonerAddressUsageUrl(prisonNumber, addressId, usageId))
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        val usageId = UUID.randomUUID().toString()
        webTestClient.delete()
          .uri(deletePrisonerAddressUsageUrl(prisonNumber, addressId, usageId))
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
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        val usageId = UUID.randomUUID().toString()
        webTestClient.delete()
          .uri(deletePrisonerAddressUsageUrl(prisonNumber, addressId, usageId))
          .exchange()
          .expectStatus()
          .isUnauthorized
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
