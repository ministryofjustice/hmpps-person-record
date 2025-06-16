package uk.gov.justice.digital.hmpps.personrecord.controller.admin

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminReclusterRequest
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.util.UUID

class AdminApiIntTest : WebTestBase() {

  @Test
  fun `should throw not found error when cluster not found in list`() {
    val uuid = UUID.randomUUID()
    val request = AdminReclusterRequest(clusters = listOf(uuid))

    webTestClient.post()
      .uri(ADMIN_RECLUSTER_URL)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isOk

    checkTelemetry(
      TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED,
      mapOf("UUID" to uuid.toString()),
      times = 0
    )
  }

  @Test
  fun `should recluster single cluster`() {
    val cluster = createPersonKey()
      .addPerson(createPerson(createRandomProbationPersonDetails()))
    val request = AdminReclusterRequest(clusters = listOf(cluster.personUUID!!))

    stubPersonMatchScores()

    webTestClient.post()
      .uri(ADMIN_RECLUSTER_URL)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isOk

    checkTelemetry(
      TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED,
      mapOf("UUID" to cluster.personUUID.toString()),
    )
  }

  @Test
  fun `should recluster list of clusters`() {
    val clusters = List(5) {
      createPersonKey()
        .addPerson(createPerson(createRandomProbationPersonDetails()))
    }
    val request = AdminReclusterRequest(clusters = clusters.map { it.personUUID!! })

    stubPersonMatchScores()

    webTestClient.post()
      .uri(ADMIN_RECLUSTER_URL)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isOk

    clusters.forEach {
      checkTelemetry(
        TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to it.personUUID.toString()),
      )
    }
  }

  companion object {
    private const val ADMIN_RECLUSTER_URL = "/admin/recluster"
  }
}
