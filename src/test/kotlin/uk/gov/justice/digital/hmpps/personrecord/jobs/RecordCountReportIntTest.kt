package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

class RecordCountReportIntTest : WebTestBase() {

  @BeforeEach
  fun beforeEach() {
    personRepository.deleteAllInBatch()
    telemetryRepository.deleteAll()
  }

  @Test
  fun `should not include merged record in telemetry`() {
    val merged = createPerson(createRandomProbationPersonDetails())
    val active = createPerson(createRandomProbationPersonDetails())
    createPerson(createRandomPrisonPersonDetails())
    createPerson(createRandomLibraPersonDetails())
    createPerson(createRandomCommonPlatformPersonDetails())

    mergeRecord(merged, active)

    webTestClient.post()
      .uri("/jobs/recordcountreport")
      .exchange()
      .expectStatus()
      .isOk

    checkTelemetry(
      TelemetryEventType.CPR_RECORD_COUNT_REPORT,
      mapOf(
        "NOMIS" to "1",
        "DELIUS" to "1",
        "COMMON_PLATFORM" to "1",
        "LIBRA" to "1",
      ),
    )
  }

  @Test
  fun `should not include passive state records in telemetry`() {
    createPerson(createRandomPrisonPersonDetails()) { markAsPassive() }
    createPerson(createRandomPrisonPersonDetails())
    createPerson(createRandomProbationPersonDetails())
    createPerson(createRandomLibraPersonDetails())
    createPerson(createRandomCommonPlatformPersonDetails())

    webTestClient.post()
      .uri("/jobs/recordcountreport")
      .exchange()
      .expectStatus()
      .isOk

    checkTelemetry(
      TelemetryEventType.CPR_RECORD_COUNT_REPORT,
      mapOf(
        "NOMIS" to "1",
        "DELIUS" to "1",
        "COMMON_PLATFORM" to "1",
        "LIBRA" to "1",
      ),
    )
  }
}
