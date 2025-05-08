package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS

@ActiveProfiles("seeding")
class PopulateEventLogIntTest : WebTestBase() {
  @Test
  fun `populate event log`() {
    createPersonWithNewKey(Person(sourceSystem = DELIUS))
    webTestClient.post()
      .uri("/populateeventlog")
      .exchange()
      .expectStatus()
      .isOk
  }
}
