package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.BROKEN_CLUSTER
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.OVERRIDE_CONFLICT
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.MERGED
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.RECLUSTER_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_PERSONAL_DETAILS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_SELF_HEALED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ReclusterServiceE2ETest : E2ETestBase() {

  @Autowired
  private lateinit var reclusterService: ReclusterService

  @Nested
  inner class ClusterAlreadySetAsNeedsAttention {

    @Test
    fun `should change from needs attention status to active when the non-matching record is updated to match the other records in the cluster above the join threshold`() {
      val basePersonData = createRandomProbationCase()

      val recordA = createProbationPerson(basePersonData)
      val matchesA = createMatchingRecord(basePersonData)
      val doesNotMatch = createProbationPerson()
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(basePersonData, crn = doesNotMatch.crn!!))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should change from needs attention status with null reason to active when the non-matching record is updated to match the other records in the cluster above the join threshold`() {
      val basePersonData = createRandomProbationCase()

      val recordA = createProbationPerson(basePersonData)
      val matchesA = createMatchingRecord(basePersonData)
      val doesNotMatch = createProbationPerson()
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = null)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(basePersonData, crn = doesNotMatch.crn!!))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should change from needs attention status to active when the non-matching record is updated to match the other records in the cluster above the fracture threshold`() {
      val basePersonData = createRandomProbationCase()

      val recordA = createProbationPerson(basePersonData)
      val matchesA = createMatchingRecord(basePersonData)
      val doesNotMatch = createProbationPerson()
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(basePersonData.aboveFracture(), doesNotMatch.crn!!))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should retain needs attention status when a record is updated which continues to match only one record out of 2 in the cluster`() {
      val basePersonData = createRandomProbationCase()

      val recordA = createProbationPerson(basePersonData)
      val matchesA = createMatchingRecord(basePersonData)

      val doesNotMatch = createProbationPerson()
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(basePersonData.withChangedMatchDetails(), recordA.crn))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
    }

    @Test
    fun `should change from needs attention status to active when the non-matching record is updated to match the other records in the cluster plus another one which is added to the cluster`() {
      val basePersonData = createRandomProbationCase()
      val recordA = createProbationPerson(basePersonData)
      val matchesA = createMatchingRecord(basePersonData)

      val doesNotMatch = createProbationPerson()

      val recordToJoinCluster = createMatchingRecord(basePersonData)
      createPersonKey().addPerson(recordToJoinCluster)
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(basePersonData, doesNotMatch.crn))

      recordToJoinCluster.assertLinkedToCluster(cluster)

      cluster.assertClusterIsOfSize(4)
      cluster.assertClusterStatus(ACTIVE)
      recordToJoinCluster.personKey?.assertMergedTo(cluster)
    }

    @Test
    fun `should not set a cluster to active if it is set to needs attention and an update does change the cluster composition`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val doesNotMatch = createProbationPerson()
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(personA)
        .addPerson(doesNotMatch)

      val personCCrn = randomCrn()
      probationDomainEventAndResponseSetup(eventType = NEW_OFFENDER_CREATED, ApiResponseSetup.from(basePersonData, personCCrn))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(basePersonData.withChangedMatchDetails(), personCCrn))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
    }

    @Test
    fun `should set a broken cluster to active if the cluster has one record in it after a delete`() {
      val basePersonData = createRandomProbationCase()

      val recordA = createProbationPerson(basePersonData)
      val recordToDelete = createMatchingRecord(basePersonData)
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(recordA)
        .addPerson(recordToDelete)
      publishProbationDomainEvent(OFFENDER_DELETION, recordToDelete.crn!!)

      cluster.assertClusterIsOfSize(1)
      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(basePersonData.withChangedMatchDetails(), crn = recordA.crn))

      cluster.assertClusterIsOfSize(1)
      cluster.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should retain needs attention if the cluster has one record in it after a delete but was a override conflict`() {
      val basePersonData = createRandomProbationCase()

      val recordA = createProbationPerson(basePersonData)
      val recordToDelete = createMatchingRecord(basePersonData)
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = OVERRIDE_CONFLICT)
        .addPerson(recordA)
        .addPerson(recordToDelete)

      publishProbationDomainEvent(OFFENDER_DELETION, recordToDelete.crn!!)

      cluster.assertClusterIsOfSize(1)
      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = OVERRIDE_CONFLICT)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(basePersonData.withChangedMatchDetails()))

      cluster.assertClusterIsOfSize(1)
      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = OVERRIDE_CONFLICT)
    }
  }

  @Nested
  inner class ClusterWithExclusionOverride {

    @Test
    fun `should merge matched clusters taking into account the exclusion marker`() {
      /*
      This test uses events to set up data
      Given three person records are created A B C
      And A and C match
      And A is merged into C
      And B is merged into C
      And B is unmerged from C
      And A is unmerged from C
      And B is updated to match A and C
      Then A joins B on the same cluster
      And C does not because it is excluded
       */
      val personAData = createRandomProbationCase()
      val personACrn = personAData.identifiers.crn!!
      val personBData = createRandomProbationCase()
      val personBCrn = personBData.identifiers.crn!!
      val personCCrn = randomCrn()
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup.from(personAData))
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup.from(personBData))
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup.from(personAData, personCCrn))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, personACrn, personCCrn)
      probationMergeEventAndResponseSetup(OFFENDER_MERGED, personBCrn, personCCrn)

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, personACrn, personCCrn, reactivatedSetup = ApiResponseSetup.from(personAData))
      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, personBCrn, personCCrn, reactivatedSetup = ApiResponseSetup.from(personBData))

      val clusterA = awaitNotNull { personRepository.findByCrn(personACrn) }.personKey
      clusterA?.assertClusterIsOfSize(1)

      val clusterB = awaitNotNull { personRepository.findByCrn(personBCrn) }.personKey
      clusterB?.assertClusterIsOfSize(1)

      val clusterC = awaitNotNull { personRepository.findByCrn(personCCrn) }.personKey
      clusterC?.assertClusterIsOfSize(1)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(personAData, personBCrn))

      clusterA?.assertClusterStatus(RECLUSTER_MERGE)
      clusterA?.assertClusterIsOfSize(0)

      val updatedClusterWithPersonB = awaitNotNull { personRepository.findByCrn(personBCrn) }.personKey
      updatedClusterWithPersonB?.assertClusterIsOfSize(2)
      updatedClusterWithPersonB?.assertClusterStatus(ACTIVE)

      val updatedClusterWithPersonC = awaitNotNull { personRepository.findByCrn(personCCrn) }.personKey
      updatedClusterWithPersonC?.assertClusterIsOfSize(1)
      updatedClusterWithPersonC?.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should not merge an updated active cluster that has an exclusion marker to another matched active cluster`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val personC = createMatchingRecord(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val personD = createMatchingRecord(basePersonData)
      val personE = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personD)
        .addPerson(personE)

      excludeRecord(personB, personD)

      recluster(personA)

      cluster1.assertClusterIsOfSize(3)
      cluster2.assertClusterIsOfSize(2)

      cluster1.assertClusterStatus(NEEDS_ATTENTION, OVERRIDE_CONFLICT)
      cluster2.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should mark active cluster as needs attention when the update record exclude another record in the matched clusters`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createMatchingRecord(basePersonData)
      val cluster3 = createPersonKey()
        .addPerson(personC)

      val personD = createMatchingRecord(basePersonData)
      val cluster4 = createPersonKey()
        .addPerson(personD)

      excludeRecord(personB, personD)

      recluster(personA)

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
    fun `should mark active cluster needs attention when the update record exclude multiple records in the matched clusters`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createMatchingRecord(basePersonData)
      val cluster3 = createPersonKey()
        .addPerson(personC)

      val personD = createMatchingRecord(basePersonData)
      val cluster4 = createPersonKey()
        .addPerson(personD)

      excludeRecord(personB, personC)
      excludeRecord(personC, personD)

      recluster(personA)

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

  @Nested
  inner class ClusterWithInclusionOverride {

    @Test
    fun `should set record to active when inclusive links within cluster`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val personC = createMatchingRecord(basePersonData)
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      probationDomainEventAndResponseSetup(
        eventType = OFFENDER_PERSONAL_DETAILS_UPDATED,
        ApiResponseSetup.from(createRandomProbationCase(crn = personB.crn!!)),
      )

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)

      includeRecords(personA, personB, personC)

      probationDomainEventAndResponseSetup(
        eventType = OFFENDER_PERSONAL_DETAILS_UPDATED,
        ApiResponseSetup.from(basePersonData.withChangedMatchDetails(), crn = personA.crn!!),
      )

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(ACTIVE)
    }
  }

  @Nested
  inner class NoChangeToCluster {

    @Test
    fun `should do nothing when only matches records in cluster above fracture threshold`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData.aboveFracture())
      val personC = createMatchingRecord(basePersonData.aboveFracture())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      recluster(personA)

      cluster.assertClusterNotChanged(size = 3)
    }

    @Test
    fun `should do nothing when matches only one record in cluster with multiple records but is still a valid cluster`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val personC = createMatchingRecord(basePersonData.aboveFracture())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      recluster(personA)

      cluster.assertClusterNotChanged(size = 3)
    }

    @Test
    fun `should do nothing when there is a mutual exclusion between updated record and a matched clusters`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personC)

      excludeRecord(personA, personC)

      recluster(personA)

      cluster1.assertClusterNotChanged(size = 2)
      cluster2.assertClusterNotChanged(size = 1)
    }

    @Test
    fun `should do nothing when there is a mutual exclusion between updated record and all records on matched cluster`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createMatchingRecord(basePersonData)
      val personC = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personB)
        .addPerson(personC)

      excludeRecord(personA, personB)
      excludeRecord(personA, personC)

      recluster(personA)

      cluster1.assertClusterNotChanged(size = 1)
      cluster2.assertClusterNotChanged(size = 2)
    }

    @Test
    fun `should do nothing when match return same items from cluster with large amount of records`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val personC = createMatchingRecord(basePersonData)
      val personD = createMatchingRecord(basePersonData)
      val personE = createMatchingRecord(basePersonData)
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)
        .addPerson(personD)
        .addPerson(personE)

      recluster(personA)

      cluster.assertClusterNotChanged(size = 5)
    }

    @Test
    fun `should do nothing when cluster is valid but matches to other clusters below the join threshold`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createMatchingRecord(basePersonData.aboveFracture())
      val cluster2 = createPersonKey()
        .addPerson(personC)

      recluster(personA)

      cluster1.assertClusterNotChanged(size = 2)
      cluster2.assertClusterNotChanged(size = 1)

      cluster2.assertNotMergedTo(cluster1)
    }
  }

  @Nested
  inner class RaceConditions {

    @Test
    fun `should not merge an active cluster to a matched cluster marked as recluster merge`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey(RECLUSTER_MERGE)
        .addPerson(personB)

      recluster(personA)

      cluster1.assertClusterIsOfSize(1)
      cluster2.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)
    }

    @Test
    fun `should not merge an active cluster to a matched cluster marked as merged`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey(MERGED)
        .addPerson(personB)

      recluster(personA)

      cluster1.assertClusterIsOfSize(1)
      cluster2.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(MERGED)
    }
  }

  @Nested
  inner class ShouldMergeClusters {

    @Test
    fun `should merge two active clusters`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personC)

      recluster(personA)

      cluster1.assertClusterIsOfSize(3)
      cluster2.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
    }

    @Test
    fun `should merge 3 active clusters`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createMatchingRecord(basePersonData)
      val cluster3 = createPersonKey()
        .addPerson(personC)

      recluster(personA)

      cluster1.assertClusterIsOfSize(3)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)
      cluster3.assertClusterStatus(RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
      cluster3.assertMergedTo(cluster1)
    }

    @Test
    fun `should merge 2 active clusters when match score returns all records from the matched cluster`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createMatchingRecord(basePersonData)
      val personD = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personC)
        .addPerson(personD)

      recluster(personA)

      cluster1.assertClusterIsOfSize(4)
      cluster2.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
    }

    @Test
    fun `should merge 3 active clusters when match score returns multiple clusters with a cluster that contain unmatched records below join threshold`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createMatchingRecord(basePersonData)
      val personC = createMatchingRecord(basePersonData.aboveFracture())
      val personD = createMatchingRecord(basePersonData.aboveFracture())
      val cluster2 = createPersonKey()
        .addPerson(personB)
        .addPerson(personC)
        .addPerson(personD)

      val personE = createMatchingRecord(basePersonData)
      val personF = createMatchingRecord(basePersonData.aboveFracture())
      val cluster3 = createPersonKey()
        .addPerson(personE)
        .addPerson(personF)

      recluster(personA)

      cluster1.assertClusterIsOfSize(6)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)
      cluster3.assertClusterStatus(RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
      cluster3.assertMergedTo(cluster1)
    }

    @Test
    fun `should merge 3 active cluster if matched cluster has a override marker to unrelated record`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createMatchingRecord(basePersonData)
      val cluster3 = createPersonKey()
        .addPerson(personC)

      val doesNotMatch = createProbationPerson()
      val cluster4 = createPersonKey()
        .addPerson(doesNotMatch)

      excludeRecord(personB, doesNotMatch)

      recluster(personA)

      cluster1.assertClusterIsOfSize(3)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(0)
      cluster4.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)
      cluster3.assertClusterStatus(RECLUSTER_MERGE)
      cluster4.assertClusterStatus(ACTIVE)

      cluster2.assertMergedTo(cluster1)
      cluster3.assertMergedTo(cluster1)
    }

    @Test
    fun `should only merge active cluster to active clusters and exclude clusters marked as needs attention`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey(NEEDS_ATTENTION)
        .addPerson(personB)

      val personC = createMatchingRecord(basePersonData)
      val cluster3 = createPersonKey()
        .addPerson(personC)

      recluster(personA)

      cluster1.assertClusterIsOfSize(2)
      cluster2.assertClusterIsOfSize(1)
      cluster3.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(NEEDS_ATTENTION)
      cluster3.assertClusterStatus(RECLUSTER_MERGE)

      cluster3.assertMergedTo(cluster1)
    }

    @Test
    fun `should merge clusters when record in a cluster only match above fracture threshold and match another cluster above join`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData.aboveFracture())
      val personC = createMatchingRecord(basePersonData.aboveFracture())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val personD = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personD)

      recluster(personA)

      cluster1.assertClusterIsOfSize(4)
      cluster2.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(ACTIVE)
      cluster2.assertClusterStatus(RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
    }

    @Test
    fun `should merge to active cluster but not a needs attention cluster even if not all records matched but the cluster is valid`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val personC = createMatchingRecord(basePersonData.aboveFracture())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val personD = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personD)

      val personE = createMatchingRecord(basePersonData)
      val cluster3 = createPersonKey(status = NEEDS_ATTENTION)
        .addPerson(personE)

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

  @Nested
  inner class ShouldMarkAsNeedsAttention {

    @Test
    fun `should mark as need attention when only matches one in cluster with multiple records`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val doesNotMatch = createProbationPerson()
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(doesNotMatch)

      recluster(personA)

      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
    }

    @Test
    fun `should mark as need attention when matches no record in cluster with multiple records`() {
      val personA = createProbationPerson()
      val personB = createProbationPerson()
      val personC = createProbationPerson()
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      recluster(personA)

      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
    }

    @Test
    fun `should mark as need attention when matches less records in cluster and contains matches from another cluster`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personBDoesNotMatch = createProbationPerson()
      val personCDoesNotMatch = createProbationPerson()
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personBDoesNotMatch)
        .addPerson(personCDoesNotMatch)

      val personD = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personD)

      recluster(personA)

      cluster1.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
      cluster2.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should mark as need attention when only matches one above fracture threshold cluster with multiple records`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(
        basePersonData.aboveFracture(),
      )
      val personC = createProbationPerson()
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      recluster(personA)

      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
      cluster.assertClusterIsOfSize(3)
    }
  }

  @Nested
  inner class EventLog {

    @Test
    fun `should log record merged when 2 active clusters merge on recluster`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createMatchingRecord(basePersonData)
      val personD = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personC)
        .addPerson(personD)

      recluster(personA)

      cluster1.assertClusterIsOfSize(4)
      cluster2.assertMergedTo(cluster1)

      checkTelemetry(
        CPR_RECLUSTER_MERGE,
        mapOf(
          "FROM_UUID" to cluster2.personUUID.toString(),
          "TO_UUID" to cluster1.personUUID.toString(),
        ),
      )

      checkEventLog(personC.crn!!, CPRLogEvents.CPR_RECLUSTER_UUID_MERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster2.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(RECLUSTER_MERGE)
      }
      checkEventLogExist(personC.crn!!, CPRLogEvents.CPR_RECLUSTER_RECORD_MERGED)
      checkEventLogExist(personD.crn!!, CPRLogEvents.CPR_RECLUSTER_RECORD_MERGED)
    }

    @Test
    fun `should log needs attention when changed record has no matches`() {
      val personData = createRandomProbationCase()
      val personA = createProbationPerson(personData)
      val personB = createProbationPerson()
      val personC = createProbationPerson()
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(personData.withChangedMatchDetails(), personA.crn!!))

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to cluster.personUUID.toString()),
      )

      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)

      checkEventLog(personA.crn!!, CPRLogEvents.CPR_RECORD_UPDATED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(NEEDS_ATTENTION)
        assertThat(eventLog.statusReason).isEqualTo(BROKEN_CLUSTER)
      }
    }

    @Test
    fun `should log back to active when cluster moves from needs attention to active`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val personC = createMatchingRecord(basePersonData)
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(basePersonData.withChangedMatchDetails(), crn = personA.crn!!))

      cluster.assertClusterStatus(ACTIVE)

      checkTelemetry(
        CPR_RECLUSTER_SELF_HEALED,
        mapOf("UUID" to cluster.personUUID.toString()),
      )

      checkEventLog(personA.crn!!, CPRLogEvents.CPR_RECORD_UPDATED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(ACTIVE)
        assertThat(eventLog.statusReason).isNull()
      }
    }
  }

  @Nested
  inner class Review {

    @Test
    fun `should raise cluster for review when broken cluster and remove when self healed`() {
      val basePersonData = createRandomProbationCase()
      val personA = createProbationPerson(basePersonData)
      val personB = createMatchingRecord(basePersonData)
      val doesNotMatch = createProbationPerson()
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(doesNotMatch)

      recluster(personA)

      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)

      val review = cluster.getReview()
        .hasReviewSize(1)
        .isPrimary(cluster)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(basePersonData.aboveFracture(), doesNotMatch.crn))

      cluster.assertClusterStatus(ACTIVE)
      review.removed()
    }

    @Test
    fun `should raise cluster for review when override conflict`() {
      val basePersonData = createRandomProbationCase()

      val personA = createProbationPerson(basePersonData)
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createMatchingRecord(basePersonData)
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createMatchingRecord(basePersonData)
      val cluster3 = createPersonKey()
        .addPerson(personC)

      excludeRecord(personB, personC)

      recluster(personA)

      cluster1.assertClusterStatus(NEEDS_ATTENTION, reason = OVERRIDE_CONFLICT)

      cluster1.getReview()
        .hasReviewSize(3)
        .isPrimary(cluster1)
        .isAdditional(cluster2, cluster3)
    }
  }

  private fun recluster(person: PersonEntity) {
    personRepository.findByMatchId(person.matchId)?.let {
      reclusterService.recluster(it)
    }
  }

  private fun PersonKeyEntity.assertClusterNotChanged(size: Int) {
    assertClusterStatus(ACTIVE)
    assertClusterIsOfSize(size)
  }
}
