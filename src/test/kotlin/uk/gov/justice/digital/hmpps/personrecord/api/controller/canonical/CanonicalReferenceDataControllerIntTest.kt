package uk.gov.justice.digital.hmpps.personrecord.api.controller.canonical

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.ReferenceDataResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.ReferenceDataWithActiveResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode

class CanonicalReferenceDataControllerIntTest : WebTestBase() {

  @Nested
  @DisplayName("GET /person/reference-data/religion-codes")
  inner class GetReligionCodes {
    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return only active religion codes by default`() {
        val responseBody = webTestClient.get()
          .uri(religionCodesApiUrl())
          .authorised(listOf(API_READ_ONLY))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<ReferenceDataWithActiveResponse>>()
          .returnResult()
          .responseBody!!

        val expectedReligionCodes = ReligionCode.entries
          .filter { it.current }
          .map { ReferenceDataWithActiveResponse(it.name, it.description, it.current) }
        assertThat(responseBody).isEqualTo(expectedReligionCodes)
      }

      @Test
      fun `should return all religion codes when active is false`() {
        val responseBody = webTestClient.get()
          .uri("${religionCodesApiUrl()}?active=false")
          .authorised(listOf(API_READ_ONLY))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<ReferenceDataWithActiveResponse>>()
          .returnResult()
          .responseBody!!

        val expectedReligionCodes = ReligionCode.entries
          .map { ReferenceDataWithActiveResponse(it.name, it.description, it.current) }
        assertThat(responseBody).isEqualTo(expectedReligionCodes)
      }
    }

    @Nested
    inner class ErrorScenarios {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        webTestClient.get()
          .uri(religionCodesApiUrl())
          .authorised(listOf("UNSUPPORTED-ROLE"))
          .exchange()
          .expectStatus()
          .isForbidden
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        webTestClient.get()
          .uri(religionCodesApiUrl())
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }

    private fun religionCodesApiUrl() = "/person/reference-data/religion-codes"
  }

  @Nested
  @DisplayName("GET /person/reference-data/country-codes")
  inner class GetCountryCodes {
    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return all country codes`() {
        val responseBody = webTestClient.get()
          .uri(countryCodesApiUrl())
          .authorised(listOf(API_READ_ONLY))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<ReferenceDataResponse>>()
          .returnResult()
          .responseBody!!

        val expectedCountryCodes = CountryCode.entries
          .map { ReferenceDataResponse(it.name, it.description) }
        assertThat(responseBody).isEqualTo(expectedCountryCodes)
      }
    }

    @Nested
    inner class ErrorScenarios {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        webTestClient.get()
          .uri(countryCodesApiUrl())
          .authorised(listOf("UNSUPPORTED-ROLE"))
          .exchange()
          .expectStatus()
          .isForbidden
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        webTestClient.get()
          .uri(countryCodesApiUrl())
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }

    private fun countryCodesApiUrl() = "/person/reference-data/country-codes"
  }
}
