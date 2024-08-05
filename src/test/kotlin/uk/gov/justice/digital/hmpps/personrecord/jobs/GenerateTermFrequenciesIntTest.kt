package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PncFrequencyRepository
import java.util.concurrent.TimeUnit.SECONDS

class GenerateTermFrequenciesIntTest: WebTestBase() {

  @Autowired
  lateinit var pncFrequencyRepository: PncFrequencyRepository

  @Test
  fun `should generate and populate pnc term frequency table`() {
    webTestClient.post()
      .uri("/generatetermfrequencies")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilAsserted  {
      assertThat(pncFrequencyRepository.findAll().size).isGreaterThan(0)
    }
  }
}