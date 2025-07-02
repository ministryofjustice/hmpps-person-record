package uk.gov.justice.digital.hmpps.personrecord.controller.admin

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminReclusterRecord
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId

class ReclusterApiIntTest : WebTestBase() {

  @Nested
  inner class MissingRecord {

    @Test
    fun `should not do anything when person record not found in list`() {
      val defendantId = randomDefendantId()
      val request = listOf(AdminReclusterRecord(SourceSystemType.COMMON_PLATFORM, defendantId))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to defendantId),
        times = 0,
      )
    }

    @Test
    fun `should not recluster records that have been merged`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val mergedPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

      mergeRecord(mergedPerson, person)

      mergedPerson.assertHasLinkToCluster()
      mergedPerson.assertMergedTo(person)

      val request = listOf(AdminReclusterRecord(SourceSystemType.DELIUS, mergedPerson.crn!!))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.toString()),
        times = 0,
      )
    }
  }

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
    }

    @Test
    fun `should recluster single person record`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val request = listOf(AdminReclusterRecord(SourceSystemType.DELIUS, person.crn!!))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.personUUID.toString()),
      )
    }

    @Test
    fun `should recluster multiple person records`() {
      val recordsWithCluster = List(5) {
        createPersonWithNewKey(createRandomProbationPersonDetails())
      }
      val request = recordsWithCluster.map { AdminReclusterRecord(it.sourceSystem, it.crn!!) }

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      recordsWithCluster.forEach {
        checkTelemetry(
          TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED,
          mapOf("UUID" to it.personKey?.personUUID.toString()),
        )
      }
    }

    @Test
    fun `should set needs attention to active when cluster is valid`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails(), status = NEEDS_ATTENTION)
      val request = listOf(AdminReclusterRecord(SourceSystemType.DELIUS, person.crn!!))

      stubClusterIsValid()

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.personUUID.toString()),
      )
      person.personKey?.assertClusterStatus(ACTIVE)
    }
  }

  companion object {
    private const val ADMIN_RECLUSTER_URL = "/admin/recluster"
  }
}
