package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_ADMIN_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminEventLogSummary
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECORD_SEEDED
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.EventLogService
import java.util.UUID

class EventLogApiIntTest : WebTestBase() {

  @Autowired
  lateinit var eventLogService: EventLogService

  @Test
  fun `should not return anything if cluster does not exist`() {
    val uuid = UUID.randomUUID().toString()
    val response = webTestClient.get()
      .uri(eventLogUrl(uuid))
      .authorised(roles = listOf(PERSON_RECORD_ADMIN_READ_ONLY))
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

    eventLogService.logEvent(RecordEventLog.from(CPR_RECORD_CREATED, person))
    eventLogService.logEvent(RecordEventLog.from(CPR_UUID_CREATED, person))

    val response = getSummary(person)

    assertThat(response.uuid).isEqualTo(person.personKey?.personUUID.toString())
    assertThat(response.eventLogs.count()).isEqualTo(2)
    assertThat(response.eventLogs[0].eventType).isEqualTo(CPR_UUID_CREATED.name)
    assertThat(response.eventLogs[0].uuidStatusType).isEqualTo("ACTIVE")
    assertThat(response.eventLogs[0].sourceSystem).isEqualTo("DELIUS")
    assertThat(response.eventLogs[0].sourceSystemId).isEqualTo(person.crn)
    assertThat(response.eventLogs[1].eventType).isEqualTo(CPR_RECORD_CREATED.name)
    assertThat(response.eventLogs[1].uuidStatusType).isEqualTo("ACTIVE")
    assertThat(response.eventLogs[1].sourceSystem).isEqualTo("DELIUS")
    assertThat(response.eventLogs[1].sourceSystemId).isEqualTo(person.crn)
  }

  @Test
  fun `should return override markers`() {
    val person = createPersonWithNewKey(createRandomProbationPersonDetails())
    val otherPerson = createPersonWithNewKey(createRandomProbationPersonDetails())
    stubPersonMatchUpsert()
    excludeRecord(person, otherPerson)

    val excludedPerson = personRepository.findByCrn(person.crn!!)!!
    eventLogService.logEvent(RecordEventLog.from(CPR_RECORD_UPDATED, excludedPerson))

    val response = getSummary(person)

    assertThat(response.uuid).isEqualTo(person.personKey?.personUUID.toString())
    assertThat(response.eventLogs[0].eventType).isEqualTo(CPR_RECORD_UPDATED.name)
    assertThat(response.eventLogs[0].overrideMarker).isEqualTo(excludedPerson.overrideMarker.toString())
    assertThat(response.eventLogs[0].overrideScopes).isEqualTo(excludedPerson.overrideScopes.map{it.scope.toString()}.toTypedArray())

  }

  @Test
  fun `should return seeded event log`() {
    val person = createPersonWithNewKey(createRandomProbationPersonDetails())

    eventLogService.logEvent(RecordEventLog.from(CPR_RECORD_SEEDED, person))

    val response = getSummary(person)

    assertThat(response.uuid).isEqualTo(person.personKey?.personUUID.toString())
    assertThat(response.eventLogs.count()).isEqualTo(1)
    assertThat(response.eventLogs[0].eventType).isEqualTo(CPR_RECORD_SEEDED.name)
    assertThat(response.eventLogs[0].uuidStatusType).isEqualTo("ACTIVE")
  }

  private fun getSummary(person: PersonEntity): AdminEventLogSummary {
    val response = webTestClient.get()
      .uri(eventLogUrl(person.personKey?.personUUID.toString()))
      .authorised(roles = listOf(PERSON_RECORD_ADMIN_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(AdminEventLogSummary::class.java)
      .returnResult()
      .responseBody!!
    return response
  }
  @Test
  fun `should return Access Denied 403 when role is wrong`() {
    val expectedErrorMessage = "Forbidden: Access Denied"
    webTestClient.get()
      .uri(eventLogUrl(UUID.randomUUID().toString()))
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
    webTestClient.get()
      .uri(eventLogUrl(UUID.randomUUID().toString()))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  private fun eventLogUrl(uuid: String) = "/admin/event-log/$uuid"
}
