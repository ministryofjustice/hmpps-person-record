package uk.gov.justice.digital.hmpps.personrecord.controller

import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.api.model.SysconSyncPrisoner
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SysconSyncIntTest : WebTestBase() {

  @Test
  fun `can send PUT`() {
    val prisoner: SysconSyncPrisoner = SysconSyncPrisoner()
    webTestClient
      .put()
      .uri("/syscon-sync/" + randomPrisonNumber())
      .body(Mono.just(prisoner), SysconSyncPrisoner::class.java)
      .authorised()
      .exchange()
      .expectStatus().isCreated
  }
}
