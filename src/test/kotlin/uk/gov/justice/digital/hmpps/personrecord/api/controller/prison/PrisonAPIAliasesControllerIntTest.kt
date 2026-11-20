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
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonAliasCreateRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonAliasUpdateRequest
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonAliasNameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.util.UUID

class PrisonAPIAliasesControllerIntTest : WebTestBase() {

  @Nested
  @DisplayName("GET /person/prison/{prisonNumber}/aliases")
  inner class GetAliasesByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendGetRequestAsserted<Unit>(
          url = aliasesUrl(randomPrisonNumber()),
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
          url = aliasesUrl(randomPrisonNumber()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendGetRequestAsserted<Unit>(
          url = aliasesUrl(randomPrisonNumber()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("GET /person/prison/{prisonNumber}/aliases/{cprAliasId}")
  inner class GetAliasByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendGetRequestAsserted<Unit>(
          url = aliasUrl(randomPrisonNumber()),
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
          url = aliasUrl(randomPrisonNumber()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendGetRequestAsserted<Unit>(
          url = aliasUrl(randomPrisonNumber()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("POST /person/prison/{prisonNumber}/aliases")
  inner class CreateAliasByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendPostRequestAsserted<Unit>(
          url = aliasesUrl(randomPrisonNumber()),
          body = validCreateRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return bad request when first name is blank`() {
        sendPostRequestAsserted<String>(
          url = aliasesUrl(randomPrisonNumber()),
          body = validCreateRequest().copy(firstName = ""),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }

      @Test
      fun `should return bad request when middle name contains invalid characters`() {
        sendPostRequestAsserted<String>(
          url = aliasesUrl(randomPrisonNumber()),
          body = validCreateRequest().copy(middleNames = "J0hn"),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }

      @Test
      fun `should return bad request when sex code is invalid`() {
        sendPostRequestAsserted<String>(
          url = aliasesUrl(randomPrisonNumber()),
          body = mapOf(
            "firstName" to "John",
            "lastName" to "Smith",
            "dateOfBirth" to "1970-01-01",
            "sex" to "NOT_A_SEX",
            "isPrimary" to true,
            "userId" to "TEST",
          ),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }

      @Test
      fun `should return bad request when isPrimary is missing`() {
        sendPostRequestAsserted<String>(
          url = aliasesUrl(randomPrisonNumber()),
          body = mapOf(
            "firstName" to "John",
            "lastName" to "Smith",
            "dateOfBirth" to "1970-01-01",
            "sex" to "M",
            "userId" to "TEST",
          ),
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
          url = aliasesUrl(randomPrisonNumber()),
          body = validCreateRequest(),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<Unit>(
          url = aliasesUrl(randomPrisonNumber()),
          body = validCreateRequest(),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("PUT /person/prison/{prisonNumber}/aliases/{cprAliasId}")
  inner class UpdateAliasByPrisonNumber {

    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return not implemented`() {
        sendPutRequestAsserted<Unit>(
          url = aliasUrl(randomPrisonNumber()),
          body = validUpdateRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return bad request when last name is blank`() {
        sendPutRequestAsserted<String>(
          url = aliasUrl(randomPrisonNumber()),
          body = validUpdateRequest().copy(lastName = ""),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }

      @Test
      fun `should return bad request when userId is missing`() {
        sendPutRequestAsserted<String>(
          url = aliasUrl(randomPrisonNumber()),
          body = mapOf(
            "firstName" to "John",
            "lastName" to "Smith",
            "dateOfBirth" to "1970-01-01",
            "sex" to "M",
          ),
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
          url = aliasUrl(randomPrisonNumber()),
          body = validUpdateRequest(),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPutRequestAsserted<Unit>(
          url = aliasUrl(randomPrisonNumber()),
          body = validUpdateRequest(),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  private fun aliasesUrl(prisonNumber: String) = "/person/prison/$prisonNumber/aliases"

  private fun aliasUrl(prisonNumber: String) = "/person/prison/$prisonNumber/aliases/${UUID.randomUUID()}"

  private fun validCreateRequest() = PrisonAliasCreateRequest(
    firstName = "John",
    middleNames = "Middleone Middletwo",
    lastName = "Smith",
    dateOfBirth = LocalDate.of(1970, 1, 1),
    sex = SexCode.M,
    title = TitleCode.MR,
    ethnicity = EthnicityCode.W1,
    nameType = PrisonAliasNameType.CN,
    isPrimary = true,
    userId = "TEST",
  )

  private fun validUpdateRequest() = PrisonAliasUpdateRequest(
    firstName = "John",
    middleNames = "Middleone Middletwo",
    lastName = "Smith",
    dateOfBirth = LocalDate.of(1970, 1, 1),
    sex = SexCode.M,
    title = TitleCode.MR,
    ethnicity = EthnicityCode.W1,
    nameType = PrisonAliasNameType.CN,
    userId = "TEST",
  )
}
