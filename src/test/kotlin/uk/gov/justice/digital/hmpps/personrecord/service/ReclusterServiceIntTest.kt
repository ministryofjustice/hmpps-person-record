package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.ValidCluster
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_NEEDS_ATTENTION_TO_ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_PERSONAL_DETAILS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
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
    fun `should add a created record to a cluster if it is set to need attention`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION)
        .addPerson(personA)

      stubPersonMatchUpsert()
      stubOnePersonMatchAboveJoinThresholdMatch(matchedRecord = personA.matchId)

      val newPersonCrn = randomCrn()
      probationDomainEventAndResponseSetup(eventType = NEW_OFFENDER_CREATED, ApiResponseSetup(crn = newPersonCrn))

      checkTelemetry(
        TelemetryEventType.CPR_RECORD_CREATED,
        mapOf("CRN" to newPersonCrn),
      )

      cluster.assertClusterIsOfSize(2)
      cluster.assertClusterStatus(NEEDS_ATTENTION)
    }

    @Test
    fun `should set a cluster to active if it is set to needs attention and an update does not change the cluster composition`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION)
        .addPerson(personA)

      stubPersonMatchUpsert()
      stubClusterIsValid()
      stubOnePersonMatchAboveJoinThresholdMatch(matchedRecord = personA.matchId)

      val newPersonCrn = randomCrn()
      probationDomainEventAndResponseSetup(eventType = NEW_OFFENDER_CREATED, ApiResponseSetup(crn = newPersonCrn))

      checkTelemetry(
        TelemetryEventType.CPR_RECORD_CREATED,
        mapOf("CRN" to newPersonCrn),
      )

      cluster.assertClusterIsOfSize(2)
      cluster.assertClusterStatus(NEEDS_ATTENTION)

      stubPersonMatchUpsert()
      stubOnePersonMatchAboveJoinThresholdMatch(matchedRecord = personA.matchId)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = newPersonCrn))

      cluster.assertClusterIsOfSize(2)
      cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    }

    @Test
    fun `should retain needs attention status when a record is updated which continues to match another record in the cluster`() {
      val recordA = createPerson(createRandomProbationPersonDetails())
      val matchesA = createPerson(createRandomProbationPersonDetails())
      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      stubPersonMatchUpsert()
      stubClusterIsNotValid()
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = recordA.crn))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(NEEDS_ATTENTION)
    }

    @Test
    fun `should change from needs attention status to active when the non-matching record is updated to match the other records in the cluster`() {
      val recordA = createPerson(createRandomProbationPersonDetails())
      val matchesA = createPerson(createRandomProbationPersonDetails())
      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      stubPersonMatchUpsert()
      stubXPersonMatchHighConfidenceMatches(matchId = doesNotMatch.matchId, aboveJoin = listOf(matchesA.matchId, recordA.matchId))
      stubClusterIsValid()
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = doesNotMatch.crn))

      cluster.assertClusterIsOfSize(3)
      cluster.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should change from needs attention status to active when the non-matching record is updated to match the other records in the cluster plus another one which is added to the cluster`() {
      val recordA = createPerson(createRandomProbationPersonDetails())
      val matchesA = createPerson(createRandomProbationPersonDetails())
      val doesNotMatch = createPerson(createRandomProbationPersonDetails())
      val recordToJoinCluster = createPersonWithNewKey(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION)
        .addPerson(recordA)
        .addPerson(matchesA)
        .addPerson(doesNotMatch)

      stubPersonMatchUpsert()
      stubXPersonMatchHighConfidenceMatches(matchId = doesNotMatch.matchId, aboveJoin = listOf(matchesA.matchId, recordA.matchId, recordToJoinCluster.matchId))
      stubClusterIsValid()
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = doesNotMatch.crn))

      cluster.assertClusterIsOfSize(4)
      cluster.assertClusterStatus(ACTIVE)
      recordToJoinCluster.personKey?.assertMergedTo(cluster)
    }

    @Test
    fun `should not set a cluster to active if it is set to needs attention and an update does change the cluster composition`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION)
        .addPerson(personA)

      stubPersonMatchUpsert()
      stubClusterIsNotValid()
      val newPersonCrn = randomCrn()
      stubOnePersonMatchAboveJoinThresholdMatch(matchedRecord = personA.matchId)
      probationDomainEventAndResponseSetup(eventType = NEW_OFFENDER_CREATED, ApiResponseSetup(crn = newPersonCrn))

      checkTelemetry(
        TelemetryEventType.CPR_RECORD_CREATED,
        mapOf("CRN" to newPersonCrn),
      )

      cluster.assertClusterIsOfSize(2)
      cluster.assertClusterStatus(NEEDS_ATTENTION)

      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = newPersonCrn))

      cluster.assertClusterIsOfSize(2)
      cluster.assertClusterStatus(NEEDS_ATTENTION)
    }
  }

  @Nested
  inner class NoChangeToCluster {

    @Test
    fun `should do nothing when there is a mutual exclusion between updated record and matched clusters`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personC)

      excludeRecord(personA, personC)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterNotChanged(size = 2)
      cluster2.assertClusterNotChanged(size = 1)
    }

    @Test
    fun `should do nothing when there is a mutual exclusion between records in matched clusters`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personC)

      excludeRecord(personB, personC)

      stubPersonMatchUpsert()
      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
        ),
      )

      probationDomainEventAndResponseSetup(eventType = NEW_OFFENDER_CREATED, ApiResponseSetup(crn = personA.crn))

      cluster1.assertClusterNotChanged(size = 2)
      cluster2.assertClusterNotChanged(size = 1)
    }

    @Test
    fun `should do nothing when there is a mutual exclusion between updated record and all records on matched cluster`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personB)
        .addPerson(personC)

      excludeRecord(personA, personB)
      excludeRecord(personA, personC)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterNotChanged(size = 1)
      cluster2.assertClusterNotChanged(size = 2)
    }

    @Test
    fun `should do nothing when match return same items from cluster with no records`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)

      stubNoMatchesPersonMatch(matchId = personA.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      cluster.assertClusterNotChanged(size = 1)
    }

    @Test
    fun `should do nothing when match return same items from cluster with multiple records`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      stubOnePersonMatchAboveJoinThresholdMatch(matchId = personA.matchId, matchedRecord = personB.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      cluster.assertClusterNotChanged(size = 2)
    }

    @Test
    fun `should do nothing when match return same items from cluster with large amount of records`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val personD = createPerson(createRandomProbationPersonDetails())
      val personE = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)
        .addPerson(personD)
        .addPerson(personE)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
          personD.matchId,
          personE.matchId,
        ),
      )

      reclusterService.recluster(cluster, changedRecord = personA)

      cluster.assertClusterNotChanged(size = 5)
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
      stubOnePersonMatchAboveJoinThresholdMatch(matchId = personA.matchId, matchedRecord = personB.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      cluster.assertClusterNotChanged(size = 3)
    }
  }

  @Nested
  inner class ShouldMarkAsNeedsAttention {

    @Test
    fun `should mark as need attention when only matches one in cluster with multiple records`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      stubClusterIsNotValid()
      stubOnePersonMatchAboveJoinThresholdMatch(matchId = personA.matchId, matchedRecord = personB.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to cluster.personUUID.toString()),
      )
      cluster.assertClusterStatus(NEEDS_ATTENTION)
    }

    @Test
    fun `should mark as need attention when matches no record in cluster with 2 records and cluster not valid`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      stubNoMatchesPersonMatch(matchId = personA.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf(
          "UUID" to cluster.personUUID.toString(),
        ),
      )
      cluster.assertClusterStatus(NEEDS_ATTENTION)
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

      stubNoMatchesPersonMatch(matchId = personA.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf(
          "UUID" to cluster.personUUID.toString(),
        ),
      )
      cluster.assertClusterStatus(NEEDS_ATTENTION)
    }

    @Test
    fun `should mark as need attention when matches less records in cluster and contains matches from another cluster`() {
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

      stubOnePersonMatchAboveJoinThresholdMatch(matchId = personA.matchId, matchedRecord = personD.matchId)

      reclusterService.recluster(cluster1, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to cluster1.personUUID.toString()),
      )
      cluster1.assertClusterStatus(NEEDS_ATTENTION)
      cluster2.assertClusterStatus(UUIDStatusType.ACTIVE)
    }
  }

  @Nested
  inner class ShouldMergeClusters {

    @Test
    fun `should merge two active clusters`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personC)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(3)
      cluster2.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
      cluster2.checkReclusterMergeTelemetry(cluster1)
    }

    @Test
    fun `should prevent circular merge of clusters`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      stubOnePersonMatchAboveJoinThresholdMatch(personA.matchId, personB.matchId)

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(2)
      cluster2.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
      cluster2.checkReclusterMergeTelemetry(cluster1)

      val updatedCluster2 = personKeyRepository.findByPersonUUID(cluster2.personUUID)!!
      updatedCluster2.addPerson(personC)

      reclusterService.recluster(updatedCluster2, changedRecord = personC)

      // does nothing as cluster set for re-clustering is RECLUSTER_MERGE
      cluster1.assertClusterIsOfSize(2)
      updatedCluster2.assertClusterIsOfSize(1)
      updatedCluster2.assertMergedTo(cluster1)
      cluster1.assertNotMergedTo(updatedCluster2)
    }

    @Test
    fun `should merge active clusters when only one record from the matched cluster is returned from the match score`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val personD = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personC)
        .addPerson(personD)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(4)
      cluster2.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
      cluster2.checkReclusterMergeTelemetry(cluster1)
    }

    @Test
    fun `should merge 3 active clusters`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster3 = createPersonKey()
        .addPerson(personC)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(3)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)
      cluster3.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
      cluster2.checkReclusterMergeTelemetry(cluster1)
      cluster3.assertMergedTo(cluster1)
      cluster3.checkReclusterMergeTelemetry(cluster1)
    }

    @Test
    fun `should merge 2 active clusters when match score returns all records from the matched cluster`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val personD = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personC)
        .addPerson(personD)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
          personD.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(4)
      cluster2.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
      cluster2.checkReclusterMergeTelemetry(cluster1)
    }

    @Test
    fun `should merge 3 active clusters when match score returns multiple cluster with cluster that contain unmatched records`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val personD = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personB)
        .addPerson(personC)
        .addPerson(personD)

      val personE = createPerson(createRandomProbationPersonDetails())
      val personF = createPerson(createRandomProbationPersonDetails())
      val cluster3 = createPersonKey()
        .addPerson(personE)
        .addPerson(personF)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personE.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(6)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)
      cluster3.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
      cluster2.checkReclusterMergeTelemetry(cluster1)
      cluster3.assertMergedTo(cluster1)
      cluster3.checkReclusterMergeTelemetry(cluster1)
    }

    @Test
    fun `should merge 3 active cluster if matched cluster has a override marker to unrelated record`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster3 = createPersonKey()
        .addPerson(personC)

      val personD = createPerson(createRandomProbationPersonDetails())
      val cluster4 = createPersonKey()
        .addPerson(personD)

      excludeRecord(personB, personD)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(3)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(0)
      cluster4.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)
      cluster3.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)
      cluster4.assertClusterStatus(UUIDStatusType.ACTIVE)

      cluster2.assertMergedTo(cluster1)
      cluster2.checkReclusterMergeTelemetry(cluster1)
      cluster3.assertMergedTo(cluster1)
      cluster3.checkReclusterMergeTelemetry(cluster1)
    }

    @Test
    fun `should merge 3 active cluster if matched record in a cluster also has a record with a override marker to unrelated record`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personD = createPerson(createRandomProbationPersonDetails())
      val cluster3 = createPersonKey()
        .addPerson(personC)

      val personE = createPerson(createRandomProbationPersonDetails())
      val cluster4 = createPersonKey()
        .addPerson(personD)

      excludeRecord(personD, personE)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(3)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(0)
      cluster4.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)
      cluster3.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)
      cluster4.assertClusterStatus(UUIDStatusType.ACTIVE)

      cluster2.assertMergedTo(cluster1)
      cluster2.checkReclusterMergeTelemetry(cluster1)
      cluster3.assertMergedTo(cluster1)
      cluster3.checkReclusterMergeTelemetry(cluster1)
    }

    @Test
    fun `should not merge an active cluster to a matched cluster marked as needs attention`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey(NEEDS_ATTENTION)
        .addPerson(personB)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(1)
      cluster2.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(NEEDS_ATTENTION)
    }

    @Test
    fun `should only merge active cluster to active clusters and exclude clusters marked as needs attention`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey(NEEDS_ATTENTION)
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster3 = createPersonKey()
        .addPerson(personC)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(2)
      cluster2.assertClusterIsOfSize(1)
      cluster3.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(NEEDS_ATTENTION)
      cluster3.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)

      cluster3.assertMergedTo(cluster1)
      cluster3.checkReclusterMergeTelemetry(cluster1)
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

      stubClusterIsValid()
      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personC.matchId,
          personD.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(4)
      cluster2.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)

      cluster2.assertMergedTo(cluster1)
      cluster2.checkReclusterMergeTelemetry(cluster1)
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

      stubClusterIsValid()
      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personC.matchId,
          personD.matchId,
          personE.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(4)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)
      cluster3.assertClusterStatus(NEEDS_ATTENTION)

      cluster2.assertMergedTo(cluster1)
      cluster2.checkReclusterMergeTelemetry(cluster1)
    }
  }

  @Nested
  inner class RaceConditions {

    @Test
    fun `should not merge an active cluster to a matched cluster marked as recluster merge`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey(UUIDStatusType.RECLUSTER_MERGE)
        .addPerson(personB)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(1)
      cluster2.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)
    }

    @Test
    fun `should not merge an active cluster to a matched cluster marked as merged`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey(UUIDStatusType.MERGED)
        .addPerson(personB)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(1)
      cluster2.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.MERGED)
    }
  }

  @Nested
  inner class ClustersWithExcludeMarkers {

    @Test
    fun `should not merge an updated active cluster that has an exclusion marker to another matched active cluster`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val personD = createPerson(createRandomProbationPersonDetails())
      val personE = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personD)
        .addPerson(personE)

      excludeRecord(personB, personD)
      val updatedPersonA = personRepository.findByMatchId(personA.matchId)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
          personD.matchId,
          personE.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = updatedPersonA!!)

      cluster1.assertClusterIsOfSize(3)
      cluster2.assertClusterIsOfSize(2)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.ACTIVE)
    }

    @Test
    fun `should merge to an excluded cluster that has exclusion to the updated cluster`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster3 = createPersonKey()
        .addPerson(personC)

      excludeRecord(personA, personC)
      excludeRecord(personB, personC)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(2)
      cluster2.assertClusterIsOfSize(0)
      cluster3.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)
      cluster3.assertClusterStatus(UUIDStatusType.ACTIVE)
    }

    @Test
    fun `should mark active cluster needs attention when the update record exclude another record in the matched cluster`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster3 = createPersonKey()
        .addPerson(personC)

      val personD = createPerson(createRandomProbationPersonDetails())
      val cluster4 = createPersonKey()
        .addPerson(personD)

      excludeRecord(personB, personD)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
          personD.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      checkTelemetry(
        TelemetryEventType.CPR_RECLUSTER_MATCHED_CLUSTERS_HAS_EXCLUSIONS,
        mapOf("UUID" to cluster1.personUUID.toString()),
      )

      cluster1.assertClusterIsOfSize(1)
      cluster2.assertClusterIsOfSize(1)
      cluster3.assertClusterIsOfSize(1)
      cluster4.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(NEEDS_ATTENTION)
      cluster2.assertClusterStatus(ACTIVE)
      cluster3.assertClusterStatus(ACTIVE)
      cluster4.assertClusterStatus(ACTIVE)
    }

    @Test
    fun `should mark active cluster needs attention when the update record exclude multiple records in the matched cluster`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster3 = createPersonKey()
        .addPerson(personC)

      val personD = createPerson(createRandomProbationPersonDetails())
      val cluster4 = createPersonKey()
        .addPerson(personD)

      excludeRecord(personB, personC)
      excludeRecord(personC, personD)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
          personD.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      checkTelemetry(
        TelemetryEventType.CPR_RECLUSTER_MATCHED_CLUSTERS_HAS_EXCLUSIONS,
        mapOf("UUID" to cluster1.personUUID.toString()),
      )

      cluster1.assertClusterIsOfSize(1)
      cluster2.assertClusterIsOfSize(1)
      cluster3.assertClusterIsOfSize(1)
      cluster4.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(NEEDS_ATTENTION)
      cluster2.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster3.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster4.assertClusterStatus(UUIDStatusType.ACTIVE)
    }
  }

  @Nested
  inner class EventLog {

    @Test
    fun `should log record merged when 2 active clusters merge on recluster`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val personD = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personC)
        .addPerson(personD)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        aboveJoin = listOf(
          personB.matchId,
          personC.matchId,
          personD.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(4)
      cluster2.assertMergedTo(cluster1)

      cluster2.checkReclusterMergeTelemetry(cluster1)
      checkEventLogByUUID(cluster2.personUUID!!, CPRLogEvents.CPR_RECLUSTER_UUID_MERGED, timeout = 6) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster2.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.RECLUSTER_MERGE)
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

      stubNoMatchesPersonMatch(matchId = personA.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      cluster.assertClusterStatus(NEEDS_ATTENTION)
      checkEventLog(personA.crn!!, CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(NEEDS_ATTENTION)
      }
    }

    @Test
    fun `should log back to active when cluster moves from needs attention to active`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = NEEDS_ATTENTION)
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      stubPersonMatchUpsert()
      stubClusterIsValid()
      stubXPersonMatchHighConfidenceMatches(matchId = personA.matchId, aboveJoin = listOf(personB.matchId, personC.matchId))
      probationDomainEventAndResponseSetup(eventType = OFFENDER_PERSONAL_DETAILS_UPDATED, ApiResponseSetup(crn = personA.crn))

      cluster.assertClusterStatus(ACTIVE)
      checkEventLog(personA.crn!!, CPR_NEEDS_ATTENTION_TO_ACTIVE) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(ACTIVE)
      }
    }

    @Test
    fun `should log cluster composition when isClusterValid is false`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val clusterComposition = listOf(
        ValidCluster(listOf(personA.matchId.toString(), personB.matchId.toString())),
        ValidCluster(listOf(personC.matchId.toString())),
      )
      stubClusterIsNotValid(clusterComposition)
      stubOnePersonMatchAboveJoinThresholdMatch(matchId = personA.matchId, matchedRecord = personB.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to cluster.personUUID.toString()),
      )
      cluster.assertClusterStatus(NEEDS_ATTENTION)

      checkEventLog(personA.crn!!, CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(NEEDS_ATTENTION)
        assertThat(eventLog.clusterComposition).isEqualTo(objectMapper.writeValueAsString(clusterComposition))
      }
    }
  }

  private fun PersonKeyEntity.assertClusterNotChanged(size: Int) {
    assertClusterStatus(UUIDStatusType.ACTIVE)
    assertClusterIsOfSize(size)
  }

  private fun PersonKeyEntity.checkReclusterMergeTelemetry(mergedCluster: PersonKeyEntity) {
    checkTelemetry(
      TelemetryEventType.CPR_RECLUSTER_MERGE,
      mapOf(
        "FROM_UUID" to this.personUUID.toString(),
        "TO_UUID" to mergedCluster.personUUID.toString(),
      ),
    )
  }
}
