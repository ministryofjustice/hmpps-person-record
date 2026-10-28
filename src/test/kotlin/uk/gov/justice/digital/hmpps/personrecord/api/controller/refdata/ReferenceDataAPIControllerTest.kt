package uk.gov.justice.digital.hmpps.personrecord.api.controller.refdata

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.refdata.ReferenceDataValue
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode

class ReferenceDataAPIControllerTest : WebTestBase() {

  @Nested
  inner class ReferenceDataTypeExists {

    @Test
    fun `should return reference data grouped by type`() {
      val responseBody = sendGetRequestAsserted<Map<String, List<ReferenceDataValue>>>(
        url = referenceDataGetEndpoint("sex"),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
      )

      val expectedResponseBody = mapOf("sex" to SexCode.entries.map(ReferenceDataValue::from))
      responseBody.isEqualTo(expectedResponseBody)
    }

    @Test
    fun `should return the canonical type key for mixed case input`() {
      val responseBody = sendGetRequestAsserted<Map<String, List<ReferenceDataValue>>>(
        url = referenceDataGetEndpoint("CONTACTTYPE"),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
      )

      val expectedResponseBody = mapOf("contactType" to ReferenceDataType.CONTACT_TYPE.values.map(ReferenceDataValue::from))
      responseBody.isEqualTo(expectedResponseBody)
    }
  }

  @Nested
  inner class ReferenceDataTypeDoesNotExist {

    @Test
    fun `should return 404 not found`() {
      sendGetRequestAsserted<Unit>(
        url = referenceDataGetEndpoint("unknown"),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.NOT_FOUND,
      )
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendGetRequestAsserted<Unit>(
        url = referenceDataGetEndpoint("sex"),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendGetRequestAsserted<Unit>(
        url = referenceDataGetEndpoint("sex"),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun referenceDataGetEndpoint(type: String) = "/person/reference-data/type/$type"

  @Nested
  inner class AllReferenceDataExists {

    @Test
    fun `should return all reference data grouped by type`() {
      val responseBody = sendGetRequestAsserted<Map<String, List<ReferenceDataValue>>>(
        url = allReferenceDataGetEndpoint(),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
      )

      val expectedResponseBody = ReferenceDataType.entries.associate { type ->
        type.key to type.values.map(ReferenceDataValue::from)
      }
      responseBody.isEqualTo(expectedResponseBody)
    }
  }

  @Nested
  inner class AllReferenceDataExistsAuth {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendGetRequestAsserted<Unit>(
        url = allReferenceDataGetEndpoint(),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendGetRequestAsserted<Unit>(
        url = allReferenceDataGetEndpoint(),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun allReferenceDataGetEndpoint() = "/person/reference-data/type/alltypes"
}
