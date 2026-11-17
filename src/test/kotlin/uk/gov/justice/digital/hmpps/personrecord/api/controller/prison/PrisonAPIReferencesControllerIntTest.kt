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
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReferenceCreateRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReferenceUpdateRequest
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class PrisonAPIReferencesControllerIntTest : WebTestBase() {

  @Nested
  @DisplayName("GET /person/prison/{prisonNumber}/references")
  inner class GetReferencesByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendGetRequestAsserted<Unit>(
          url = referencesUrl(randomPrisonNumber()),
          roles = listOf(API_READ_ONLY),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }

      @Test
      fun `should return not implemented when including pseudonyms`() {
        sendGetRequestAsserted<Unit>(
          url = "${referencesUrl(randomPrisonNumber())}?includePseudonyms=true",
          roles = listOf(API_READ_ONLY),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return bad request when includePseudonyms is not a boolean`() {
        sendGetRequestAsserted<String>(
          url = "${referencesUrl(randomPrisonNumber())}?includePseudonyms=notABoolean",
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
          url = referencesUrl(randomPrisonNumber()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendGetRequestAsserted<Unit>(
          url = referencesUrl(randomPrisonNumber()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("GET /person/prison/{prisonNumber}/pseudonyms/{cprPseudonymId}/references/{cprReferenceId}")
  inner class GetPseudonymReferenceByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendGetRequestAsserted<Unit>(
          url = pseudonymReferenceUrl(randomPrisonNumber()),
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
          url = pseudonymReferenceUrl(randomPrisonNumber()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendGetRequestAsserted<Unit>(
          url = pseudonymReferenceUrl(randomPrisonNumber()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("PUT /person/prison/{prisonNumber}/pseudonyms/{cprPseudonymId}/references/{cprReferenceId}")
  inner class UpdatePseudonymReferenceByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendPutRequestAsserted<Unit>(
          url = pseudonymReferenceUrl(randomPrisonNumber()),
          body = validUpdateRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return bad request when reference is blank`() {
        sendPutRequestAsserted<String>(
          url = pseudonymReferenceUrl(randomPrisonNumber()),
          body = validUpdateRequest().copy(reference = ""),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }

      @Test
      fun `should return bad request when userId is missing`() {
        sendPutRequestAsserted<String>(
          url = pseudonymReferenceUrl(randomPrisonNumber()),
          body = mapOf("reference" to "1231/XX/121"),
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
          url = pseudonymReferenceUrl(randomPrisonNumber()),
          body = validUpdateRequest(),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPutRequestAsserted<Unit>(
          url = pseudonymReferenceUrl(randomPrisonNumber()),
          body = validUpdateRequest(),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("POST /person/prison/{prisonNumber}/references")
  inner class CreateReferencesByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendPostRequestAsserted<Unit>(
          url = referencesUrl(randomPrisonNumber()),
          body = listOf(validCreateRequest()),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return bad request when reference type is invalid`() {
        sendPostRequestAsserted<String>(
          url = referencesUrl(randomPrisonNumber()),
          body = listOf(mapOf("referenceType" to "NOT_A_TYPE", "reference" to "1231/XX/121", "userId" to "TEST")),
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
          url = referencesUrl(randomPrisonNumber()),
          body = listOf(validCreateRequest()),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<Unit>(
          url = referencesUrl(randomPrisonNumber()),
          body = listOf(validCreateRequest()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  private fun referencesUrl(prisonNumber: String) = "/person/prison/$prisonNumber/references"

  private fun pseudonymReferenceUrl(prisonNumber: String) = "/person/prison/$prisonNumber/pseudonyms/${UUID.randomUUID()}/references/${UUID.randomUUID()}"

  private fun validCreateRequest() = PrisonReferenceCreateRequest(
    referenceType = PNC,
    reference = "1231/XX/121",
    comment = "Important info",
    userId = "TEST",
  )

  private fun validUpdateRequest() = PrisonReferenceUpdateRequest(
    reference = "1231/XX/121",
    comment = "Important info",
    userId = "TEST",
  )
}
