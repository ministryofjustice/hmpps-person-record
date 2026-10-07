package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactRequest
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.BUS
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class PrisonAPIContactsControllerIntTest : WebTestBase() {

  @Nested
  @DisplayName("GET /person/prison/{prisonNumber}/contacts")
  inner class GetContactsByPrisonNumber {

    @Nested
    inner class HappyPath {
      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendGetRequestAsserted<Unit>(
          url = contactsUrl(randomPrisonNumber()),
          roles = listOf(API_READ_ONLY),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }

      @Test
      fun `should accept include and exclude type filters`() {
        sendGetRequestAsserted<Unit>(
          url = "${contactsUrl(randomPrisonNumber())}?includeTypes=HOME,MOBILE&excludeTypes=EMAIL",
          roles = listOf(API_READ_ONLY),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return bad request when include type is invalid`() {
        sendGetRequestAsserted<String>(
          url = "${contactsUrl(randomPrisonNumber())}?includeTypes=NOT_A_TYPE",
          roles = listOf(API_READ_ONLY),
          expectedStatus = BAD_REQUEST,
        )
      }

      @Test
      fun `should return bad request when exclude type is invalid`() {
        sendGetRequestAsserted<String>(
          url = "${contactsUrl(randomPrisonNumber())}?excludeTypes=NOT_A_TYPE",
          roles = listOf(API_READ_ONLY),
          expectedStatus = BAD_REQUEST,
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendGetRequestAsserted<String>(
          url = contactsUrl(randomPrisonNumber()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendGetRequestAsserted<Unit>(
          url = contactsUrl(randomPrisonNumber()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("POST /person/prison/{prisonNumber}/contacts")
  inner class CreateContactByPrisonNumber {

    @Nested
    inner class HappyPath {
      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendPostRequestAsserted<Unit>(
          url = contactsUrl(randomPrisonNumber()),
          body = validContactRequest(),
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
          url = contactsUrl(randomPrisonNumber()),
          body = invalidContactRequest(),
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
          url = contactsUrl(randomPrisonNumber()),
          body = validContactRequest(),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<Unit>(
          url = contactsUrl(randomPrisonNumber()),
          body = validContactRequest(),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("PUT /person/prison/{prisonNumber}/contacts/{contactId}")
  inner class UpdateContactByPrisonNumber {

    @Nested
    inner class HappyPath {
      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendPutRequestAsserted<Unit>(
          url = contactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validContactRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return bad request when value is missing`() {
        sendPutRequestAsserted<String>(
          url = contactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = invalidContactRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPutRequestAsserted<String>(
          url = contactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validContactRequest(),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPutRequestAsserted<Unit>(
          url = contactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validContactRequest(),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  private fun contactsUrl(prisonNumber: String) = "/person/prison/$prisonNumber/contacts"
  private fun contactUrl(prisonNumber: String, contactId: String) = "/person/prison/$prisonNumber/contacts/$contactId"

  private fun validContactRequest() = PrisonContactRequest(
    type = BUS,
    value = "01234 567 890",
    extension = "123",
    userId = "TEST",
  )

  private fun invalidContactRequest() = mapOf(
    "type" to BUS,
    "userId" to "TEST",
  )
}
