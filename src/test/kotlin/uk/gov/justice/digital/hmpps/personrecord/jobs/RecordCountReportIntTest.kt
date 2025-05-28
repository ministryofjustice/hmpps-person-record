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
  fun `should report record count to telemetry`() {
    createPerson(createRandomProbationPersonDetails())
    createPerson(createRandomPrisonPersonDetails())
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
