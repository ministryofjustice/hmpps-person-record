package uk.gov.justice.digital.hmpps.personrecord.controller.syscon

import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconSyncIntTest : WebTestBase() {

  @Test
  fun `can send PUT`() {
    val prisoner = Prisoner()
    webTestClient
      .put()
      .uri("/syscon-sync/" + randomPrisonNumber())
      .body(Mono.just(prisoner), Prisoner::class.java)
      .authorised()
      .exchange()
      .expectStatus().isOk
  }
}
