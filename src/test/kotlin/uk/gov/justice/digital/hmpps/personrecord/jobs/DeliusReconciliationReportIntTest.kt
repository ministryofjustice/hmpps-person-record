package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.web.PagedModel.PageMetadata
import uk.gov.justice.digital.hmpps.personrecord.client.ProbationCases
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

class DeliusReconciliationReportIntTest : WebTestBase() {

  @BeforeEach
  fun beforeEach() {
    deleteAllPersonData()
    telemetryRepository.deleteAll()
  }

  @Test
  fun `tracks person count between delius and cpr`() {
    createPersonWithNewKey(createRandomProbationPersonDetails())

    val responseBody = ProbationCases(
      page = PageMetadata(1, 0, 0, 5),
    )
    stubGetRequest(
      url = "/all-probation-cases?page=0&size=1&sort=id,asc",
      body = jsonMapper.writeValueAsString(responseBody),
    )

    webTestClient.post()
      .uri("/jobs/deliusreconciliationreport")
      .exchange()
      .expectStatus()
      .isOk

    checkTelemetry(
      TelemetryEventType.CPR_RECORD_DELIUS_RECONCILIATION_REPORT,
      mapOf(
        "DELIUS" to "5",
        "CPR" to "1",
      ),
    )
  }
}
