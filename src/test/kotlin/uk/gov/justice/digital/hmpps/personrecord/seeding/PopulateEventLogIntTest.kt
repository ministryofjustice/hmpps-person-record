package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents

@ActiveProfiles("seeding")
class PopulateEventLogIntTest : WebTestBase() {
  @Test
  fun `populate event log`() {
    eventLogRepository.deleteAll()
    personKeyRepository.deleteAll()
    val key = createPersonKey()
    val person = createPerson(Person(sourceSystem = DELIUS), key)
    webTestClient.post()
      .uri("/populateeventlog")
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert { assertThat(eventLogRepository.count()).isEqualTo(1) }
    val entry = eventLogRepository.findAll().first()
    assertThat(entry.uuid).isEqualTo(key.personUUID)
    assertThat(entry.sourceSystem).isEqualTo(DELIUS)
    assertThat(entry.eventType).isEqualTo(CPRLogEvents.CPR_RECORD_CREATED)
    assertThat(entry.sourceSystemId).isEqualTo(person.crn)
  }
}
