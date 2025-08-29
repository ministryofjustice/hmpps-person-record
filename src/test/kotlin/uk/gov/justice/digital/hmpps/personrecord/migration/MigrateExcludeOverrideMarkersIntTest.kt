package uk.gov.justice.digital.hmpps.personrecord.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class MigrateExcludeOverrideMarkersIntTest : WebTestBase() {

  @BeforeEach
  fun beforeEach() {
    personRepository.deleteAllInBatch()
  }

  @Test
  fun `should migrate exclude override markers to new override scopes`() {
    val cluster = createPersonKey()
    val firstRecord = createPerson(createRandomProbationPersonDetails(), cluster)
    val secondRecord = createPerson(createRandomProbationPersonDetails())

    excludeRecord(firstRecord, secondRecord, newProcess = false)

    firstRecord.assertDoesNotHaveOverrideMarker()
    secondRecord.assertDoesNotHaveOverrideMarker()

    firstRecord.assertExcludedFrom(secondRecord)

    stubPersonMatchUpsert()

    webTestClient.post()
      .uri("/migrate/exclude-markers")
      .exchange()
      .expectStatus()
      .isOk

    firstRecord.assertHasOverrideMarker()
    secondRecord.assertHasOverrideMarker()

    firstRecord.assertHasSameOverrideScope(secondRecord)
  }

  @Test
  fun `should not overwrite existing override markers`() {
    val cluster = createPersonKey()
    val firstRecord = createPerson(createRandomProbationPersonDetails(), cluster)
    val secondRecord = createPerson(createRandomProbationPersonDetails())

    excludeRecord(firstRecord, secondRecord)

    firstRecord.assertHasOverrideMarker()
    secondRecord.assertHasOverrideMarker()

    val initialFirstRecordOverrideMarker = awaitNotNullPerson { personRepository.findByMatchId(firstRecord.matchId) }.overrideMarker
    val initialSecondRecordOverrideMarker = awaitNotNullPerson { personRepository.findByMatchId(secondRecord.matchId) }.overrideMarker

    webTestClient.post()
      .uri("/migrate/exclude-markers")
      .exchange()
      .expectStatus()
      .isOk

    firstRecord.assertHasSameOverrideScope(secondRecord)
    firstRecord.assertOverrideScopeSize(1)
    secondRecord.assertOverrideScopeSize(1)

    val finalFirstRecordOverrideMarker = awaitNotNullPerson { personRepository.findByMatchId(firstRecord.matchId) }.overrideMarker
    val finalSecondRecordOverrideMarker = awaitNotNullPerson { personRepository.findByMatchId(secondRecord.matchId) }.overrideMarker

    assertThat(initialFirstRecordOverrideMarker).isEqualTo(finalFirstRecordOverrideMarker)
    assertThat(initialSecondRecordOverrideMarker).isEqualTo(finalSecondRecordOverrideMarker)
  }
}
