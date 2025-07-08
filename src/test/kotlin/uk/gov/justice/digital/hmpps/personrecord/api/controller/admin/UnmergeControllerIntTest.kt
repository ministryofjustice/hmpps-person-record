package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class UnmergeControllerIntTest : WebTestBase() {

  @BeforeEach
  fun beforeEach() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
  }

  @Test
  fun `should unmerge 2 records that have been merged`() {
    val reactivatedCrn = randomCrn()
    val unmergedCrn = randomCrn()

    val cluster = createPersonKey()
    val unmergedPerson = createPerson(createRandomProbationPersonDetails(unmergedCrn), cluster)
    val reactivatedPerson = createPerson(createRandomProbationPersonDetails(reactivatedCrn))

    mergeRecord(reactivatedPerson, unmergedPerson)
    val request = AdminUnmergeRequest(unmergedCrn, reactivatedCrn)

    webTestClient.post()
      .uri("/admin/unmerge")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isOk

    checkEventLogExist(reactivatedPerson.crn!!, CPRLogEvents.CPR_UUID_CREATED)
    checkEventLog(reactivatedPerson.crn!!, CPRLogEvents.CPR_RECORD_UNMERGED) { eventLogs ->
      assertThat(eventLogs).hasSize(1)
      val eventLog = eventLogs.first()
      assertThat(eventLog.personUUID).isNotEqualTo(cluster.personUUID)
      assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
      assertThat(eventLog.excludeOverrideMarkers).contains(unmergedPerson.id)
    }
    checkEventLog(unmergedPerson.crn!!, CPRLogEvents.CPR_RECORD_UPDATED) { eventLogs ->
      assertThat(eventLogs).hasSize(1)
      val eventLog = eventLogs.first()
      assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
      assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
      assertThat(eventLog.excludeOverrideMarkers).contains(reactivatedPerson.id)
    }

    checkTelemetry(
      CPR_UUID_CREATED,
      mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
    )

    checkTelemetry(
      CPR_RECORD_UNMERGED,
      mapOf(
        "TO_SOURCE_SYSTEM_ID" to reactivatedCrn,
        "FROM_SOURCE_SYSTEM_ID" to unmergedCrn,
        "UNMERGED_UUID" to cluster.personUUID.toString(),
        "SOURCE_SYSTEM" to "DELIUS",
      ),
    )

    unmergedPerson.assertHasLinkToCluster()
    unmergedPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
    unmergedPerson.personKey?.assertClusterIsOfSize(1)
    unmergedPerson.assertExcludedFrom(reactivatedPerson)

    reactivatedPerson.assertHasLinkToCluster()
    reactivatedPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
    reactivatedPerson.personKey?.assertClusterIsOfSize(1)
    reactivatedPerson.assertNotLinkedToCluster(unmergedPerson.personKey!!)
    reactivatedPerson.assertExcludedFrom(unmergedPerson)
    reactivatedPerson.assertNotMerged()
  }
}
