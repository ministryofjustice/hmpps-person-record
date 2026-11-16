package uk.gov.justice.digital.hmpps.personrecord.api.controller.refdata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.refdata.ReferenceDataValue
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode

class ReferenceDataAPIControllerTest : WebTestBase() {

  @Test
  fun `should return prison address usages with applicability in response body`() {
    val response = sendGetRequestAsserted<Map<String, List<ReferenceDataValue>>>(
      url = "${referenceDataGetEndpoint("addressUsage")}?applicableTo=PRISON",
      roles = listOf(API_READ_ONLY),
      expectedStatus = HttpStatus.OK,
    )
    response.isEqualTo(
      mapOf("addressUsage" to AddressUsageCode.entries.filter { ReferenceDataDomain.PRISON in it.applicableTo }.map(ReferenceDataValue::from)),
    )
    response.consumeWith {
      val values = it.responseBody!!.getValue("addressUsage")
      assertThat(values.map { value -> value.code }).contains("HOME").doesNotContain("A02", "UNKNOWN")
      values.forEach { value -> assertThat(value.applicableTo).containsExactly(ReferenceDataDomain.PRISON) }
    }
  }

  @Test
  fun `should return all types filtered for probation with shared values`() {
    val response = sendGetRequestAsserted<Map<String, List<ReferenceDataValue>>>(
      url = "${allReferenceDataGetEndpoint()}?applicableTo=PROBATION",
      roles = listOf(API_READ_ONLY),
      expectedStatus = HttpStatus.OK,
    )
    response.isEqualTo(
      ReferenceDataType.entries.associate { type ->
        type.key to type.values.filter { ReferenceDataDomain.PROBATION in it.applicableTo }.map(ReferenceDataValue::from)
      },
    )
    response.consumeWith {
      val all = it.responseBody!!
      assertThat(all.getValue("addressUsage").map { value -> value.code }).contains("A02").doesNotContain("HOME", "UNKNOWN")
      assertThat(all.getValue("sex")).hasSize(SexCode.entries.size)
      all.values.flatten().forEach { value -> assertThat(value.applicableTo).contains(ReferenceDataDomain.PROBATION) }
    }
  }

  @Test
  fun `should support each domain on both endpoints`() {
    ReferenceDataDomain.entries.forEach { domain ->
      sendGetRequestAsserted<Map<String, List<ReferenceDataValue>>>(
        url = "${referenceDataGetEndpoint("addressUsage")}?applicableTo=$domain",
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
      ).isEqualTo(
        mapOf("addressUsage" to AddressUsageCode.entries.filter { domain in it.applicableTo }.map(ReferenceDataValue::from)),
      )
      sendGetRequestAsserted<Map<String, List<ReferenceDataValue>>>(
        url = "${allReferenceDataGetEndpoint()}?applicableTo=$domain",
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
      ).isEqualTo(
        ReferenceDataType.entries.associate { type ->
          type.key to type.values.filter { domain in it.applicableTo }.map(ReferenceDataValue::from)
        },
      )
    }
  }

  @Test
  fun `should reject invalid domains on both endpoints`() {
    listOf(referenceDataGetEndpoint("addressUsage"), allReferenceDataGetEndpoint()).forEach { endpoint ->
      sendGetRequestAsserted<Unit>(
        url = "$endpoint?applicableTo=INVALID",
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.BAD_REQUEST,
      )
    }
  }

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
