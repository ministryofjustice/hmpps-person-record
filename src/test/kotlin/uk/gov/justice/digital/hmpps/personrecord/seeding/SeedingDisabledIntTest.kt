package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.health.PersonMatchHealthPing
import uk.gov.justice.digital.hmpps.personrecord.health.PersonRecordHealthPing

class SeedingDisabledIntTest : WebTestBase() {

  @MockBean
  private lateinit var personMatchHealthPing: PersonMatchHealthPing

  @MockBean
  @Autowired
  private lateinit var personRecordHealthPing: PersonRecordHealthPing

  @Test
  fun `populate from prison endpoint not accessible when seeding is not enabled`() {
    webTestClient.post()
      .uri("/populatefromprison")
      .exchange()
      .expectStatus()
      .isUnauthorized // strange - should be a 404 I think.
  }

  @Test
  fun `populate from probation endpoint not accessible when seeding is not enabled`() {
    webTestClient.post()
      .uri("/populatefromprobation")
      .exchange()
      .expectStatus()
      .isUnauthorized // strange - should be a 404 I think.
  }
}
