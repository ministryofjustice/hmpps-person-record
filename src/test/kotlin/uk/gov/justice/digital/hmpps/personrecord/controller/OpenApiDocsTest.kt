package uk.gov.justice.digital.hmpps.personrecord.controller

import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import org.junit.jupiter.api.Test

class OpenApiDocsTest: WebTestBase() {
  @LocalServerPort
  private var port: Int = 0

  @Test
  fun `open api docs are available`() {
    webTestClient.get()
      .uri("/swagger-ui/index.html")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
  }
}