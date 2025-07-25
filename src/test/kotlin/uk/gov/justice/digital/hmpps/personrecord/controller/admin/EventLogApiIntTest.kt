package uk.gov.justice.digital.hmpps.personrecord.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminEventLogSummary
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.EventLogService
import java.util.UUID

class EventLogApiIntTest : WebTestBase() {

  @Autowired
  lateinit var eventLogService: EventLogService

  @Test
  fun `should return not return anything if cluster does not exist`() {
    val uuid = UUID.randomUUID().toString()
    val response = webTestClient.get()
      .uri(eventLogUrl(uuid))
      .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(AdminEventLogSummary::class.java)
      .returnResult()
      .responseBody!!

    assertThat(response.uuid).isEqualTo(uuid)
    assertThat(response.eventLogs.count()).isEqualTo(0)
  }

  @Test
  fun `should return list of event logs`() {
    val person = createPersonWithNewKey(createRandomProbationPersonDetails())

    eventLogService.logEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_CREATED, person))
    eventLogService.logEvent(RecordEventLog(CPRLogEvents.CPR_UUID_CREATED, person))

    val response = webTestClient.get()
      .uri(eventLogUrl(person.personKey?.personUUID.toString()))
      .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(AdminEventLogSummary::class.java)
      .returnResult()
      .responseBody!!

    assertThat(response.uuid).isEqualTo(person.personKey?.personUUID.toString())
    assertThat(response.eventLogs.count()).isEqualTo(2)
    assertThat(response.eventLogs[0].eventType).isEqualTo(CPRLogEvents.CPR_UUID_CREATED.name)
    assertThat(response.eventLogs[0].uuidStatusType).isEqualTo("ACTIVE")
    assertThat(response.eventLogs[0].sourceSystem).isEqualTo("DELIUS")
    assertThat(response.eventLogs[0].sourceSystemId).isEqualTo(person.crn)
    assertThat(response.eventLogs[1].eventType).isEqualTo(CPRLogEvents.CPR_RECORD_CREATED.name)
    assertThat(response.eventLogs[1].uuidStatusType).isEqualTo("ACTIVE")
    assertThat(response.eventLogs[1].sourceSystem).isEqualTo("DELIUS")
    assertThat(response.eventLogs[1].sourceSystemId).isEqualTo(person.crn)
  }

  @Test
  fun `should return seeded event log`() {
    val person = createPersonWithNewKey(createRandomProbationPersonDetails())

    eventLogService.logEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_SEEDED, person))

    val response = webTestClient.get()
      .uri(eventLogUrl(person.personKey?.personUUID.toString()))
      .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(AdminEventLogSummary::class.java)
      .returnResult()
      .responseBody!!

    assertThat(response.uuid).isEqualTo(person.personKey?.personUUID.toString())
    assertThat(response.eventLogs.count()).isEqualTo(1)
    assertThat(response.eventLogs[0].eventType).isEqualTo(CPRLogEvents.CPR_RECORD_SEEDED.name)
    assertThat(response.eventLogs[0].uuidStatusType).isEqualTo("ACTIVE")
  }

  @Test
  fun `should return Access Denied 403 when role is wrong`() {
    val person = createPersonWithNewKey(createRandomProbationPersonDetails())
    val expectedErrorMessage = "Forbidden: Access Denied"
    webTestClient.get()
      .uri(eventLogUrl(person.personKey?.personUUID.toString()))
      .authorised(listOf("UNSUPPORTED-ROLE"))
      .exchange()
      .expectStatus()
      .isForbidden
      .expectBody()
      .jsonPath("userMessage")
      .isEqualTo(expectedErrorMessage)
  }

  @Test
  fun `should return UNAUTHORIZED 401 when role is not set`() {
    val person = createPersonWithNewKey(createRandomProbationPersonDetails())
    webTestClient.get()
      .uri(eventLogUrl(person.personKey?.personUUID.toString()))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  private fun eventLogUrl(uuid: String) = "/admin/event-log/$uuid"
}
