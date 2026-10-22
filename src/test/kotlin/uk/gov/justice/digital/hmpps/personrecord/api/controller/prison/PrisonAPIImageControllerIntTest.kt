package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestSetup

class PrisonAPIImageControllerIntTest : WebTestSetup() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return image bytes with status 200`() {
      val prisonNumber = "A1234AA"
      val imageBytes = byteArrayOf(
        0xFF.toByte(),
        0xD8.toByte(),
        0xFF.toByte(),
        0xD9.toByte(),
      )

      stubPrisonerImage(
        offenderNo = prisonNumber,
        status = 200,
        imageBytes = imageBytes,
      )

      val response = webTestClient.get()
        .uri(imageUrl(prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType(MediaType.IMAGE_JPEG)
        .expectBody<ByteArray>()
        .returnResult()
        .responseBody!!

      assertThat(response).isEqualTo(imageBytes)
    }
  }

  @Nested
  inner class ErrorScenarios {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      val offenderNo = "A1234AA"

      webTestClient.get()
        .uri(imageUrl(offenderNo))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return 404 when upstream image is not found`() {
      val prisonNumber = "A1234AA"

      stubPrisonerImage(
        offenderNo = prisonNumber,
        status = 404,
      )

      webTestClient.get()
        .uri(imageUrl(prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return 500 when upstream returns an internal server error`() {
      val prisonNumber = "A1234AA"

      stubPrisonerImage(
        offenderNo = prisonNumber,
        status = 500,
      )

      webTestClient.get()
        .uri(imageUrl(prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .is5xxServerError
    }

    @Test
    fun `should return 500 when upstream image response body is empty`() {
      val prisonNumber = "A1234AA"

      stubPrisonerImage(
        offenderNo = prisonNumber,
        status = 200,
        imageBytes = byteArrayOf(),
      )

      webTestClient.get()
        .uri(imageUrl(prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .is5xxServerError
    }
  }

  private fun imageUrl(offenderNo: String) = "/person/prison/$offenderNo/image"
}
