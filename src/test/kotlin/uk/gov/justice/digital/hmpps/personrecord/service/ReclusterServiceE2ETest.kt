package uk.gov.justice.digital.hmpps.personrecord.service

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.ValidCluster
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
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_PERSONAL_DETAILS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ReclusterServiceE2ETest : E2ETestBase() {

  @Autowired
  private lateinit var reclusterService: ReclusterService

  @Nested
  inner class ClusterAlreadySetAsNeedsAttention {

    @Test
    fun `should change from needs attention status to active when the non-matching record is updated to match the other records in the cluster above the join threshold`() {
      val basePersonData = createRandomProbationPersonDetails()

      val recordA = createPerson(createProbationPersonFrom(basePersonData))
      val matchesA = createPerson(createProbationPersonFrom(basePersonData))
      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      val nowMatchesA = createProbationPersonFrom(basePersonData, crn = doesNotMatch.crn!!)
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(nowMatchesA))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should change from needs attention status to active when the non-matching record is updated to match the other records in the cluster above the fracture threshold`() {
      val basePersonData = createRandomProbationPersonDetails()

      val recordA = createPerson(createProbationPersonFrom(basePersonData))
      val matchesA = createPerson(createProbationPersonFrom(basePersonData))
      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      val nowMatchesAboveFracture = createProbationPersonFrom(basePersonData, doesNotMatch.crn!!).aboveFracture() // makes sure this still isnt a join weight
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(nowMatchesAboveFracture))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should retain needs attention status when a record is updated which continues to match only one record out of 2 in the cluster`() {
      val basePersonData = createRandomProbationPersonDetails()

      val recordAPersonData = createProbationPersonFrom(basePersonData)
      val recordA = createPerson(recordAPersonData)
      val matchesA = createPerson(createProbationPersonFrom(basePersonData))

      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      val updatedRecordAData = recordAPersonData.withChangedMatchDetails()
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(updatedRecordAData))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
    }

    @Test
    fun `should change from needs attention status to active when the non-matching record is updated to match the other records in the cluster plus another one which is added to the cluster`() {
      val basePersonData = createRandomProbationPersonDetails()
      val recordA = createPerson(createProbationPersonFrom(basePersonData))
      val matchesA = createPerson(createProbationPersonFrom(basePersonData))

      val doesNotMatch = createPerson(createRandomProbationPersonDetails())

      val recordToJoinCluster = createPersonWithNewKey(createProbationPersonFrom(basePersonData))
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      val nowMatchesAPersonData = createProbationPersonFrom(basePersonData, crn = doesNotMatch.crn!!)
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(nowMatchesAPersonData))

      recordToJoinCluster.assertLinkedToCluster(cluster)

      cluster.assertClusterIsOfSize(4)
      cluster.assertClusterStatus(ACTIVE)
      recordToJoinCluster.personKey?.assertMergedTo(cluster)
    }

    @Test
    fun `should not set a cluster to active if it is set to needs attention and an update does change the cluster composition`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
        .addPerson(personA)
        .addPerson(doesNotMatch)

      val newPersonCData = createProbationPersonFrom(basePersonData)
      probationDomainEventAndResponseSetup(eventType = NEW_OFFENDER_CREATED, ApiResponseSetup.from(newPersonCData))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)

      val updatedPersonCData = newPersonCData.withChangedMatchDetails()
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(updatedPersonCData))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
    }
  }

  @Nested
  inner class ClusterWithExclusionOverride {

    @Test
    fun `should merge matched clusters taking into account the exclusion marker`() {
      val personAData = createRandomProbationPersonDetails()
      val personBData = createRandomProbationPersonDetails()
      val personCData = personAData

      val personA = createProbationPersonFrom(personAData)
      val personB = createProbationPersonFrom(personBData)
      val personC = createProbationPersonFrom(personCData)

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, personA.crn!!, personC.crn!!, reactivatedSetup = ApiResponseSetup.from(personA))
      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, personB.crn!!, personC.crn, reactivatedSetup = ApiResponseSetup.from(personB))

      val clusterA = awaitNotNullPerson { personRepository.findByCrn(personA.crn) }.personKey
      clusterA?.assertClusterIsOfSize(1)

      val clusterB = awaitNotNullPerson { personRepository.findByCrn(personB.crn) }.personKey
      clusterB?.assertClusterIsOfSize(1)

      val clusterC = awaitNotNullPerson { personRepository.findByCrn(personC.crn) }.personKey
      clusterC?.assertClusterIsOfSize(1)

      val updateWithDataFromPersonAAndCrnFromPersonB = personA.copy(crn = personB.crn)
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup.from(updateWithDataFromPersonAAndCrnFromPersonB))

      val updateClusterWithPersonA = awaitNotNullPerson { personRepository.findByCrn(personA.crn) }.personKey
      updateClusterWithPersonA?.assertClusterIsOfSize(2)
      updateClusterWithPersonA?.assertClusterStatus(ACTIVE)

      val updateClusterWithPersonB = awaitNotNullPerson { personRepository.findByCrn(personB.crn) }.personKey
      updateClusterWithPersonB?.assertClusterIsOfSize(2)
      updateClusterWithPersonB?.assertClusterStatus(ACTIVE)

      val updateClusterWithPersonC = awaitNotNullPerson { personRepository.findByCrn(personC.crn) }.personKey
      updateClusterWithPersonC?.assertClusterIsOfSize(1)
      updateClusterWithPersonC?.assertClusterStatus(ACTIVE)

      clusterA?.assertClusterStatus(RECLUSTER_MERGE)
      clusterA?.assertClusterIsOfSize(0)
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
    fun `should mark active cluster as needs attention when the update record exclude another record in the matched clusters`() {
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
    fun `should mark active cluster needs attention when the update record exclude multiple records in the matched clusters`() {
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

  @Nested
  inner class NoChangeToCluster {

    @Test
    fun `should do nothing when only matches records in cluster above fracture threshold`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(
        createProbationPersonFrom(basePersonData)
          .aboveFracture(),
      )
      val personC = createPerson(
        createProbationPersonFrom(basePersonData)
          .aboveFracture(),
      )
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      recluster(personA)

      cluster.assertClusterNotChanged(size = 3)
    }

    @Test
    fun `should do nothing when matches only one record in cluster with multiple records but is still a valid cluster`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val personC = createPerson(
        createProbationPersonFrom(basePersonData)
          .aboveFracture(),
      )
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      recluster(personA)

      cluster.assertClusterNotChanged(size = 3)
    }

    @Test
    fun `should do nothing when there is a mutual exclusion between updated record and a matched clusters`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createProbationPersonFrom(basePersonData))
      val cluster2 = createPersonKey()
        .addPerson(personC)

      excludeRecord(personA, personC)

      recluster(personA)

      cluster1.assertClusterNotChanged(size = 2)
      cluster2.assertClusterNotChanged(size = 1)
    }

    @Test
    fun `should do nothing when there is a mutual exclusion between records in matched clusters`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createProbationPersonFrom(basePersonData))
      val cluster2 = createPersonKey()
        .addPerson(personC)

      excludeRecord(personB, personC)

      recluster(personA)

      cluster1.assertClusterNotChanged(size = 2)
      cluster2.assertClusterNotChanged(size = 1)
    }

    @Test
    fun `should do nothing when there is a mutual exclusion between updated record and all records on matched cluster`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val personC = createPerson(createProbationPersonFrom(basePersonData))
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
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val personC = createPerson(createProbationPersonFrom(basePersonData))
      val personD = createPerson(createProbationPersonFrom(basePersonData))
      val personE = createPerson(createProbationPersonFrom(basePersonData))
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
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(
        createProbationPersonFrom(basePersonData)
          .aboveFracture(),
      )
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
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createProbationPersonFrom(basePersonData))
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
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createProbationPersonFrom(basePersonData))
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
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createProbationPersonFrom(basePersonData))
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
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createProbationPersonFrom(basePersonData))
      val personD = createPerson(createProbationPersonFrom(basePersonData))
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
    fun `should merge 3 active clusters when match score returns multiple clusters with a cluster that contain unmatched records`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val personC = createPerson(createRandomProbationPersonDetails())
      val personD = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personB)
        .addPerson(personC)
        .addPerson(personD)

      val personE = createPerson(createProbationPersonFrom(basePersonData))
      val personF = createPerson(createRandomProbationPersonDetails())
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

      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
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
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val cluster2 = createPersonKey(NEEDS_ATTENTION)
        .addPerson(personB)

      val personC = createPerson(createProbationPersonFrom(basePersonData))
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
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(
        createProbationPersonFrom(basePersonData)
          .aboveFracture(),
      )
      val personC = createPerson(
        createProbationPersonFrom(basePersonData)
          .aboveFracture(),
      )
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val personD = createPerson(createProbationPersonFrom(basePersonData))
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
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val personC = createPerson(
        createProbationPersonFrom(basePersonData)
          .aboveFracture(),
      )
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val personD = createPerson(createProbationPersonFrom(basePersonData))
      val cluster2 = createPersonKey()
        .addPerson(personD)

      val personE = createPerson(createProbationPersonFrom(basePersonData))
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
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(doesNotMatch)

      recluster(personA)

      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
    }

    @Test
    fun `should mark as need attention when matches no record in cluster with multiple records`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      recluster(personA)

      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
    }

    @Test
    fun `should mark as need attention when matches less records in cluster and contains matches from another cluster`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personBDoesNotMatch = createPerson(createRandomProbationPersonDetails())
      val personCDoesNotMatch = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personBDoesNotMatch)
        .addPerson(personCDoesNotMatch)

      val personD = createPerson(createProbationPersonFrom(basePersonData))
      val cluster2 = createPersonKey()
        .addPerson(personD)

      recluster(personA)

      cluster1.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
      cluster2.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should mark as need attention when only matches one above fracture threshold cluster with multiple records`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(
        createProbationPersonFrom(basePersonData)
          .aboveFracture(),
      )
      val personC = createPerson(createRandomProbationPersonDetails())
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
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createProbationPersonFrom(basePersonData))
      val personD = createPerson(createProbationPersonFrom(basePersonData))
      val cluster2 = createPersonKey()
        .addPerson(personC)
        .addPerson(personD)

      recluster(personA)

      cluster1.assertClusterIsOfSize(4)
      cluster2.assertMergedTo(cluster1)

      checkTelemetry(
        TelemetryEventType.CPR_RECLUSTER_MERGE,
        mapOf(
          "FROM_UUID" to cluster2.personUUID.toString(),
          "TO_UUID" to cluster1.personUUID.toString(),
        ),
      )

      checkEventLogByUUID(cluster2.personUUID!!, CPRLogEvents.CPR_RECLUSTER_UUID_MERGED, timeout = 6) { eventLogs ->
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
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      recluster(personA)

      checkTelemetry(
        TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to cluster.personUUID.toString()),
      )

      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
      checkEventLog(personA.crn!!, CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(NEEDS_ATTENTION)
        assertThat(eventLog.statusReason).isEqualTo(BROKEN_CLUSTER)
      }
    }

    @Test
    fun `should log back to active when cluster moves from needs attention to active`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val personC = createPerson(createProbationPersonFrom(basePersonData))
      val cluster = createPersonKey(status = NEEDS_ATTENTION)
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      cluster.assertClusterStatus(NEEDS_ATTENTION)

      recluster(personA)

      checkEventLog(personA.crn!!, CPRLogEvents.CPR_NEEDS_ATTENTION_TO_ACTIVE) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(ACTIVE)
        assertThat(eventLog.statusReason).isNull()
      }

      cluster.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should log cluster composition when isClusterValid is false`() {
      val basePersonData = createRandomProbationPersonDetails()

      val personA = createPerson(createProbationPersonFrom(basePersonData))
      val personB = createPerson(createProbationPersonFrom(basePersonData))
      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(doesNotMatch)

      recluster(personA)

      checkTelemetry(
        TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to cluster.personUUID.toString()),
      )

      cluster.assertClusterStatus(NEEDS_ATTENTION, reason = BROKEN_CLUSTER)

      val clusterComposition = listOf(
        ValidCluster(listOf(doesNotMatch.matchId.toString())),
        ValidCluster(listOf(personA.matchId.toString(), personB.matchId.toString())),
      )
      checkEventLog(personA.crn!!, CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(NEEDS_ATTENTION)
        assertThat(eventLog.statusReason).isEqualTo(BROKEN_CLUSTER)
        eventLog.clusterComposition.assertHasClusterComposition(clusterComposition)
      }
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

  private fun String?.assertHasClusterComposition(expectedClusterComposition: List<ValidCluster>) = this?.let {
    val actualClusterComposition = objectMapper.readValue<List<ValidCluster>>(this)
    assertThat(actualClusterComposition.map { it.records.toSet() }.toSet()).isEqualTo(expectedClusterComposition.map { it.records.toSet() }.toSet())
  }
}
