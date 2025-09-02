package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.OVERRIDE_CONFLICT
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.RECLUSTER_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

class ReclusterServiceE2ETest : E2ETestBase() {

  @Autowired
  private lateinit var reclusterService: ReclusterService

  @Nested
  inner class ClusterWithExclusionOverride {

    @Test
    fun `should merge to an excluded cluster that has exclusion to the updated cluster`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createPerson(createProbationPersonFrom(basePersonData))
      val cluster3 = createPersonKey()
        .addPerson(personC)

      excludeRecord(personA, personC)
      excludeRecord(personB, personC)

      recluster(personA)

      cluster1.assertClusterIsOfSize(2)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)
      cluster3.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should not merge an updated active cluster that has an exclusion marker to another matched active cluster`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val personC = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val personD = createPerson(createProbationPersonFrom(basePersonData))
      val personE = createPerson(createProbationPersonFrom(basePersonData))
      val cluster2 = createPersonKey()
        .addPerson(personD)
        .addPerson(personE)

      excludeRecord(personB, personD)

      recluster(personA)

      cluster1.assertClusterIsOfSize(3)
      cluster2.assertClusterIsOfSize(2)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should mark active cluster needs attention when the update record exclude another record in the matched cluster`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createPerson(createProbationPersonFrom(basePersonData))
      val cluster3 = createPersonKey()
        .addPerson(personC)

      val personD = createPerson(createProbationPersonFrom(basePersonData))
      val cluster4 = createPersonKey()
        .addPerson(personD)

      excludeRecord(personB, personD)

      recluster(personA)

      checkTelemetry(
        TelemetryEventType.CPR_RECLUSTER_MATCHED_CLUSTERS_HAS_EXCLUSIONS,
        mapOf("UUID" to cluster1.personUUID.toString()),
      )

      cluster1.assertClusterIsOfSize(1)
      cluster2.assertClusterIsOfSize(1)
      cluster3.assertClusterIsOfSize(1)
      cluster4.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(NEEDS_ATTENTION, reason = OVERRIDE_CONFLICT)
      cluster2.assertClusterStatus(ACTIVE)
      cluster3.assertClusterStatus(ACTIVE)
      cluster4.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should mark active cluster needs attention when the update record exclude multiple records in the matched cluster`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createPerson(createProbationPersonFrom(basePersonData))
      val cluster3 = createPersonKey()
        .addPerson(personC)

      val personD = createPerson(createProbationPersonFrom(basePersonData))
      val cluster4 = createPersonKey()
        .addPerson(personD)

      excludeRecord(personB, personC)
      excludeRecord(personC, personD)

      recluster(personA)

      checkTelemetry(
        TelemetryEventType.CPR_RECLUSTER_MATCHED_CLUSTERS_HAS_EXCLUSIONS,
        mapOf("UUID" to cluster1.personUUID.toString()),
      )

      cluster1.assertClusterIsOfSize(1)
      cluster2.assertClusterIsOfSize(1)
      cluster3.assertClusterIsOfSize(1)
      cluster4.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(NEEDS_ATTENTION, reason = OVERRIDE_CONFLICT)
      cluster2.assertClusterStatus(ACTIVE)
      cluster3.assertClusterStatus(ACTIVE)
      cluster4.assertClusterStatus(ACTIVE)
    }
  }

  private fun recluster(person: PersonEntity) {
    personRepository.findByMatchId(person.matchId)?.let {
      reclusterService.recluster(it)
    }
  }
}
