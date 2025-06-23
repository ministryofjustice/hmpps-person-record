package uk.gov.justice.digital.hmpps.personrecord.controller.admin

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminDeleteRecord
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class DeleteApiIntTest : WebTestBase() {
  @Test
  fun `should handle not found person`() {
    val crn = randomCrn()
    val request = listOf(AdminDeleteRecord(SourceSystemType.DELIUS, crn))

    webTestClient.post()
      .uri(ADMIN_DELETE_URL)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isOk

    checkTelemetry(
      TelemetryEventType.CPR_RECORD_DELETED,
      mapOf("CRN" to crn),
      times = 0,
    )
  }

  companion object {
    private const val ADMIN_DELETE_URL = "/admin/delete"
  }
}