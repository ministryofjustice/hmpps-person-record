package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminDeleteRecord
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_DELETED
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

  @Test
  fun `should delete a single record`() {
    stubDeletePersonMatch()

    val person = createPersonWithNewKey(createRandomProbationPersonDetails())
    val crn = person.crn!!
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
      times = 1,
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to crn, "UUID" to person.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkEventLog(crn, CPRLogEvents.CPR_UUID_DELETED) { eventLogs ->
      assertThat(eventLogs).hasSize(1)
      val eventLog = eventLogs.first()
      assertThat(eventLog.personUUID).isEqualTo(person.personKey?.personUUID)
    }
    checkEventLog(crn, CPRLogEvents.CPR_RECORD_DELETED) { eventLogs ->
      assertThat(eventLogs).hasSize(1)
      val eventLog = eventLogs.first()
      assertThat(eventLog.personUUID).isEqualTo(person.personKey?.personUUID)
    }

    person.assertPersonDeleted()
    person.personKey?.assertPersonKeyDeleted()
  }

  companion object {
    private const val ADMIN_DELETE_URL = "/admin/delete"
  }
}
