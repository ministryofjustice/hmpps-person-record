package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.BROKEN_CLUSTER
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.RECLUSTER_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_PERSONAL_DETAILS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

/**
 * Test that cover recluster scenarios.
 * Based of scenarios in:
 * https://dsdmoj.atlassian.net/wiki/spaces/PRD/pages/5607784556/Recluster+Process+Flow
 */
class ReclusterServiceIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  private lateinit var reclusterService: ReclusterService

  @Nested
  inner class ClusterAlreadySetAsNeedsAttention {

    @Test
    fun `should change from needs attention status to active when the non-matching record is updated to match the other records in the cluster above the join threshold`() {
      val recordA = createPerson(createRandomProbationPersonDetails())
      val matchesA = createPerson(createRandomProbationPersonDetails())
      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      stubPersonMatchUpsert()
      stubXPersonMatches(matchId = doesNotMatch.matchId, aboveJoin = listOf(matchesA.matchId, recordA.matchId))
      stubClusterIsValid()
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = doesNotMatch.crn))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should change from needs attention status to active when the non-matching record is updated to match the other records in the cluster above the fracture threshold`() {
      val recordA = createPerson(createRandomProbationPersonDetails())
      val matchesA = createPerson(createRandomProbationPersonDetails())
      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      stubPersonMatchUpsert()
      stubXPersonMatches(matchId = doesNotMatch.matchId, aboveFracture = listOf(matchesA.matchId, recordA.matchId))
      stubClusterIsValid(clusters = listOf(recordA.matchId, matchesA.matchId, doesNotMatch.matchId))
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = doesNotMatch.crn))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(ACTIVE)
    }
  }

  @Nested
  inner class NoChangeToCluster {

    @Test
    fun `should do nothing when match return same items from cluster with multiple records which are above join threshold`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      stubOnePersonMatchAboveJoinThreshold(matchId = personA.matchId, matchedRecord = personB.matchId)

      recluster(personA)

      cluster.assertClusterNotChanged(size = 2)
    }

    @Test
    fun `should do nothing when match return same items from cluster with multiple records which are above fracture threshold`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      stubOnePersonMatchAboveFractureThreshold(matchId = personA.matchId, matchedRecord = personB.matchId)

      recluster(personA)

      cluster.assertClusterNotChanged(size = 2)
    }

    @Test
    fun `should do nothing when match return same items from cluster with multiple records above join and above fracture`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      stubXPersonMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(personB.matchId),
        aboveFracture = listOf(personC.matchId),
      )

      recluster(personA)

      cluster.assertClusterNotChanged(size = 3)
    }

    @Test
    fun `should do nothing when matches only one record in cluster with multiple records but is still a valid cluster`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      stubClusterIsValid()
      stubOnePersonMatchAboveJoinThreshold(matchId = personA.matchId, matchedRecord = personB.matchId)

      recluster(personA)

      cluster.assertClusterNotChanged(size = 3)
    }

    @Test
    fun `should do nothing when cluster is valid but matches to other clusters below the join threshold`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personC)

      stubXPersonMatches(
        matchId = personA.matchId,
        aboveFracture = listOf(personB.matchId, personC.matchId),
      )

      recluster(personA)

      cluster1.assertClusterNotChanged(size = 2)
      cluster2.assertClusterNotChanged(size = 1)

      cluster2.assertNotMergedTo(cluster1)
    }
  }

  @Nested
  inner class ShouldMarkAsNeedsAttention {

    @Test
    fun `should mark as need attention when only matches one above fracture threshold cluster with multiple records`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      stubClusterIsNotValid()
      stubXPersonMatches(matchId = personA.matchId, aboveFracture = listOf(personB.matchId), belowFracture = listOf(personC.matchId))

      recluster(personA)

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to cluster.personUUID.toString()),
      )
      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
    }
  }

  @Nested
  inner class ShouldMergeClusters {

    @Test
    fun `should merge clusters when record in a cluster only match above fracture threshold and match another cluster above join`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personC)

      stubXPersonMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personC.matchId,
        ),
        aboveFracture = listOf(
          personB.matchId,
        ),
      )

      recluster(personA)

      cluster1.assertClusterIsOfSize(3)
      cluster2.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
    }

    @Test
    fun `should merge to active cluster even if not all records matched but the cluster is valid`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val personD = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personD)

      stubClusterIsValid(clusters = listOf(personA.matchId, personB.matchId, personC.matchId))
      stubXPersonMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personC.matchId,
          personD.matchId,
        ),
      )

      recluster(personA)

      cluster1.assertClusterIsOfSize(4)
      cluster2.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
    }

    @Test
    fun `should merge to active cluster but not a needs attention cluster even if not all records matched but the cluster is valid`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val personD = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personD)

      val personE = createPerson(createRandomProbationPersonDetails())
      val cluster3 = createPersonKey(status = NEEDS_ATTENTION)
        .addPerson(personE)

      stubClusterIsValid(clusters = listOf(personA.matchId, personB.matchId, personC.matchId))
      stubXPersonMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personC.matchId,
          personD.matchId,
          personE.matchId,
        ),
      )

      recluster(personA)

      cluster1.assertClusterIsOfSize(4)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)
      cluster3.assertClusterStatus(NEEDS_ATTENTION)

      cluster2.assertMergedTo(cluster1)
    }
  }

  private fun recluster(personA: PersonEntity) {
    personRepository.findByMatchId(personA.matchId)?.let {
      reclusterService.recluster(it)
    }
  }

  private fun PersonKeyEntity.assertClusterNotChanged(size: Int) {
    assertClusterStatus(ACTIVE)
    assertClusterIsOfSize(size)
  }
}
