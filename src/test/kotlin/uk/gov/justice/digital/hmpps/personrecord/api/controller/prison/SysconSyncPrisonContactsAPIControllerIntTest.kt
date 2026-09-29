package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconContactMapping
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonContactsAPIControllerIntTest : WebTestBase() {

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
        sendPutRequestAsserted<Unit>(
          url = updatePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
        sendPutRequestAsserted<String>(
          url = updatePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
        sendPutRequestAsserted<Unit>(
          url = updatePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
          url = deletePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  private fun createPrisonerContactUrl(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/contact"
  private fun createPrisonerAddressContactUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId/contact"
  private fun updatePrisonerContactUrl(prisonNumber: String, contactId: String) = "/syscon-sync/person/$prisonNumber/contact/$contactId"
  private fun deletePrisonerContactUrl(prisonNumber: String, contactId: String) = "/syscon-sync/person/$prisonNumber/contact/$contactId"
}
