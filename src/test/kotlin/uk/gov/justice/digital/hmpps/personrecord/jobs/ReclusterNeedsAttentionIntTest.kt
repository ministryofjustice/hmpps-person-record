package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_SELF_HEALED

class ReclusterNeedsAttentionIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      personKeyRepository.deleteAll()
    }

    @Test
    fun `should recluster a needs attention cluster`() {
      val activePerson = createPersonWithNewKey(createRandomProbationPersonDetails())
      val needsAttentionPerson = createPersonWithNewKey(createRandomProbationPersonDetails(), status = UUIDStatusType.NEEDS_ATTENTION)

      stubPersonMatchScores(matchId = needsAttentionPerson.matchId)

      webTestClient.post()
        .uri("/jobs/recluster-needs-attention")
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_RECLUSTER_SELF_HEALED,
        mapOf("UUID" to needsAttentionPerson.personKey?.personUUID.toString()),
      )

      activePerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      needsAttentionPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
    }
  }
}
