package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminReclusterRecord
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.BROKEN_CLUSTER
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED
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
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
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

      val request = listOf(AdminReclusterRecord(DELIUS, mergedPerson.crn!!))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.toString()),
        times = 0,
      )
    }
  }

  @Nested
  inner class ErrorRecovery {

    @Test
    fun `should retry if request to hmpps-person-match fails`() {
      stubPersonMatchUpsert()
      val person = createPersonWithNewKey(createRandomProbationPersonDetails(), status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
      val request = listOf(AdminReclusterRecord(DELIUS, person.crn!!))
      stub5xxResponse(url = "/person/score/" + person.matchId)
      stubPersonMatchUpsert(currentScenarioState = "Next request will succeed")
      stubPersonMatchScores(
        matchId = person.matchId,
        personMatchResponse = listOf(),
        currentScenarioState = "Next request will succeed",
      )

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk
      person.personKey?.assertClusterStatus(ACTIVE)
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
      val request = listOf(AdminReclusterRecord(DELIUS, person.crn!!))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
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
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      recordsWithCluster.forEach {
        checkTelemetry(
          CPR_ADMIN_RECLUSTER_TRIGGERED,
          mapOf("UUID" to it.personKey?.personUUID.toString()),
        )
      }
    }

    @Test
    fun `should set needs attention to active when cluster is valid`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails(), status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
      val request = listOf(AdminReclusterRecord(DELIUS, person.crn!!))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.personUUID.toString()),
      )
      person.personKey?.assertClusterStatus(ACTIVE)
    }
  }

  companion object {
    private const val ADMIN_RECLUSTER_URL = "/admin/recluster"
  }
}
