package uk.gov.justice.digital.hmpps.personrecord.api.controller

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class OpenApiDocsTest : WebTestBase() {

  @Test
  fun `open api docs are available`() {
    webTestClient.get()
      .uri("/swagger-ui/index.html")
      .exchange()
      .expectStatus().isOk
  }
}
