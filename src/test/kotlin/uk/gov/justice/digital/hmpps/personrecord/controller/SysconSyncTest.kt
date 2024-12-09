package uk.gov.justice.digital.hmpps.personrecord.controller

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconSyncTest : WebTestBase() {

  @Test
  fun `can send PUT`() {
    webTestClient
      .put()
      .uri("/syscon-sync/" + randomPrisonNumber())
      .authorised()
      .exchange()
      .expectStatus().isCreated
  }
}
