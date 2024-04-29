package uk.gov.justice.digital.hmpps.personrecord.populatefromnomis

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import java.util.concurrent.TimeUnit.SECONDS

class PopulateFromNomisIntTest : IntegrationTestBase() {

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
    val prisoners = defendantRepository.findAll()
    assertThat(prisoners[0].firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(prisoners[1].firstName).isEqualTo("PrisonerTwoFirstName")
    assertThat(prisoners[2].firstName).isEqualTo("PrisonerThreeFirstName")
    assertThat(prisoners[3].firstName).isEqualTo("PrisonerFourFirstName")
    assertThat(prisoners[4].firstName).isEqualTo("PrisonerFiveFirstName")
    assertThat(prisoners[5].firstName).isEqualTo("PrisonerSixFirstName")
    assertThat(prisoners[6].firstName).isEqualTo("PrisonerSevenFirstName")
  }
}
