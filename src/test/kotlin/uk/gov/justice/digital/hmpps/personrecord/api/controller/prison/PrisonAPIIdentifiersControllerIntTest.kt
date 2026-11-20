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
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonIdentifierCreateRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonIdentifierUpdateRequest
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class PrisonAPIIdentifiersControllerIntTest : WebTestBase() {

  @Nested
  @DisplayName("GET /person/prison/{prisonNumber}/identifiers")
  inner class GetIdentifiersByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendGetRequestAsserted<Unit>(
          url = identifiersUrl(randomPrisonNumber()),
          roles = listOf(API_READ_ONLY),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }

      @Test
      fun `should return not implemented when including aliases`() {
        sendGetRequestAsserted<Unit>(
          url = "${identifiersUrl(randomPrisonNumber())}?includeAliases=true",
          roles = listOf(API_READ_ONLY),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return bad request when includeAliases is not a boolean`() {
        sendGetRequestAsserted<String>(
          url = "${identifiersUrl(randomPrisonNumber())}?includeAliases=notABoolean",
          roles = listOf(API_READ_ONLY),
          expectedStatus = BAD_REQUEST,
        )
      }
    }

    @Nested
    inner class Auth {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendGetRequestAsserted<String>(
          url = identifiersUrl(randomPrisonNumber()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendGetRequestAsserted<Unit>(
          url = identifiersUrl(randomPrisonNumber()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("GET /person/prison/{prisonNumber}/aliases/{cprAliasId}/identifiers/{cprIdentifierId}")
  inner class GetAliasIdentifierByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendGetRequestAsserted<Unit>(
          url = aliasIdentifierUrl(randomPrisonNumber()),
          roles = listOf(API_READ_ONLY),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Auth {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendGetRequestAsserted<String>(
          url = aliasIdentifierUrl(randomPrisonNumber()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendGetRequestAsserted<Unit>(
          url = aliasIdentifierUrl(randomPrisonNumber()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("PUT /person/prison/{prisonNumber}/aliases/{cprAliasId}/identifiers/{cprIdentifierId}")
  inner class UpdateAliasIdentifierByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendPutRequestAsserted<Unit>(
          url = aliasIdentifierUrl(randomPrisonNumber()),
          body = validUpdateRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return bad request when identifier is blank`() {
        sendPutRequestAsserted<String>(
          url = aliasIdentifierUrl(randomPrisonNumber()),
          body = validUpdateRequest().copy(identifier = ""),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }

      @Test
      fun `should return bad request when userId is missing`() {
        sendPutRequestAsserted<String>(
          url = aliasIdentifierUrl(randomPrisonNumber()),
          body = mapOf("identifier" to "1231/XX/121"),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }
    }

    @Nested
    inner class Auth {
      @Test
      fun `should return Access Denied 403 when role is read only`() {
        sendPutRequestAsserted<String>(
          url = aliasIdentifierUrl(randomPrisonNumber()),
          body = validUpdateRequest(),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPutRequestAsserted<Unit>(
          url = aliasIdentifierUrl(randomPrisonNumber()),
          body = validUpdateRequest(),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("POST /person/prison/{prisonNumber}/identifiers")
  inner class CreateIdentifiersByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendPostRequestAsserted<Unit>(
          url = identifiersUrl(randomPrisonNumber()),
          body = listOf(validCreateRequest()),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return bad request when identifier type is invalid`() {
        sendPostRequestAsserted<String>(
          url = identifiersUrl(randomPrisonNumber()),
          body = listOf(mapOf("identifierType" to "NOT_A_TYPE", "identifier" to "1231/XX/121", "userId" to "TEST")),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }
    }

    @Nested
    inner class Auth {
      @Test
      fun `should return Access Denied 403 when role is read only`() {
        sendPostRequestAsserted<String>(
          url = identifiersUrl(randomPrisonNumber()),
          body = listOf(validCreateRequest()),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<Unit>(
          url = identifiersUrl(randomPrisonNumber()),
          body = listOf(validCreateRequest()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  private fun identifiersUrl(prisonNumber: String) = "/person/prison/$prisonNumber/identifiers"

  private fun aliasIdentifierUrl(prisonNumber: String) = "/person/prison/$prisonNumber/aliases/${UUID.randomUUID()}/identifiers/${UUID.randomUUID()}"

  private fun validCreateRequest() = PrisonIdentifierCreateRequest(
    identifierType = PNC,
    identifier = "1231/XX/121",
    comment = "Important info",
    userId = "TEST",
  )

  private fun validUpdateRequest() = PrisonIdentifierUpdateRequest(
    identifier = "1231/XX/121",
    comment = "Important info",
    userId = "TEST",
  )
}
