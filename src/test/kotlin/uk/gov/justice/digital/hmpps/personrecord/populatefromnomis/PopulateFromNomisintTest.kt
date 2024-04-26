package uk.gov.justice.digital.hmpps.personrecord.populatefromnomis

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import java.util.concurrent.TimeUnit.SECONDS

class PopulateFromNomisintTest : IntegrationTestBase() {

  @Test
  fun `populate from nomis`() {
    webTestClient.post()
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    // will have to change to personrepository but this will be near enough
    val defendants = await.atMost(30, SECONDS) untilNotNull {
      defendantRepository.findAll()
    }
    assertThat(defendants.size).isEqualTo(7)
  }
}
