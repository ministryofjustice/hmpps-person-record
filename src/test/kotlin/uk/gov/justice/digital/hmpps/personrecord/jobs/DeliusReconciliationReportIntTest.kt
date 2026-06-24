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
  fun `cpr and delius have same person count`() {
    createPersonWithNewKey(createRandomProbationPersonDetails())

    val responseBody = ProbationCases(
      page = PageMetadata(1, 0, 0, 1),
      cases = listOf(createRandomProbationCase()),
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
      mapOf("DELIUS" to "1"),
    )
  }
}
