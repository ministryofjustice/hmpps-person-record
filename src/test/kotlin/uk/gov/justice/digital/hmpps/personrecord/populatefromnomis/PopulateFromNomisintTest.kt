package uk.gov.justice.digital.hmpps.personrecord.populatefromnomis

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
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
    await.atMost(15, SECONDS) untilAsserted {
      assertThat(defendantRepository.findAll().size).isEqualTo(7)
    }
  }
}
