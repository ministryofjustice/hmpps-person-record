package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.TransactionalReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_SELF_HEALED

class ReclusterNeedsAttentionJobIntTest(
  @Autowired val transactionalReclusterService: TransactionalReclusterService,
) : IntegrationTestBase() {

  @Test
  fun `should recluster a needs attention cluster`() {
    val activePerson = createPersonWithNewKey(createRandomProbationPersonDetails())
    val needsAttentionPerson = createPersonWithNewKey(createRandomProbationPersonDetails(), status = NEEDS_ATTENTION)

    stubPersonMatchScores(matchId = needsAttentionPerson.matchId)
    val reclusterNeedsAttentionJob = ReclusterNeedsAttentionJob(personKeyRepository, transactionalReclusterService)
    reclusterNeedsAttentionJob.run()

    checkTelemetry(
      CPR_RECLUSTER_SELF_HEALED,
      mapOf("UUID" to needsAttentionPerson.personKey?.personUUID.toString()),
    )

    activePerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
    needsAttentionPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
  }
}
