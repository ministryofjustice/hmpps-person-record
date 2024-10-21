package uk.gov.justice.digital.hmpps.personrecord.controller

import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import org.junit.jupiter.api.Test

class OpenApiDocsTest: WebTestBase() {

  @Test
  fun `open api docs are available`() {
    webTestClient.get()
      .uri("/swagger-ui/index.html")
      .exchange()
      .expectStatus().isOk
  }
}