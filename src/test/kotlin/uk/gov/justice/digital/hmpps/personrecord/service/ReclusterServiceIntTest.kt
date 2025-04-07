package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
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
      val cluster = createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION)
        .addPerson(personA)

      stubPersonMatchUpsert()
      stubOnePersonMatchHighConfidenceMatch(matchedRecord = personA.matchId)

      val newPersonCrn = randomCrn()
      probationDomainEventAndResponseSetup(eventType = NEW_OFFENDER_CREATED, ApiResponseSetup(crn = newPersonCrn))

      checkTelemetry(
        TelemetryEventType.CPR_RECORD_CREATED,
        mapOf("CRN" to newPersonCrn),
      )

      cluster.assertClusterIsOfSize(2)
      cluster.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)
    }

    @Test
    fun `should update record and do nothing when cluster already set as needs attention`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION)
        .addPerson(personA)

      stubPersonMatchUpsert()
      probationDomainEventAndResponseSetup(eventType = NEW_OFFENDER_CREATED, ApiResponseSetup(crn = personA.crn))

      checkTelemetry(
        TelemetryEventType.CPR_RECORD_UPDATED,
        mapOf("CRN" to personA.crn),
      )
      checkTelemetry(
        TelemetryEventType.CPR_RECLUSTER_UUID_MARKED_NEEDS_ATTENTION,
        mapOf("UUID" to cluster.personId.toString()),
      )
    }
  }

  @Nested
  inner class NoChangeToCluster {

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

      stubOnePersonMatchHighConfidenceMatch(matchId = personA.matchId, matchedRecord = personB.matchId)

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
        results = listOf(
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
      stubOnePersonMatchHighConfidenceMatch(matchId = personA.matchId, matchedRecord = personB.matchId)

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
      stubOnePersonMatchHighConfidenceMatch(matchId = personA.matchId, matchedRecord = personB.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to cluster.personId.toString()),
      )
      cluster.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)
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
          "UUID" to cluster.personId.toString(),
        ),
      )
      cluster.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)
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
          "UUID" to cluster.personId.toString(),
        ),
      )
      cluster.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)
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

      stubOnePersonMatchHighConfidenceMatch(matchId = personA.matchId, matchedRecord = personD.matchId)

      reclusterService.recluster(cluster1, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to cluster1.personId.toString()),
      )
      cluster1.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)
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
        results = listOf(
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
        results = listOf(
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
        results = listOf(
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
      cluster3.assertMergedTo(cluster1)
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
        results = listOf(
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
        results = listOf(
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
      cluster3.assertMergedTo(cluster1)
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
        results = listOf(
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
      cluster3.assertMergedTo(cluster1)
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
        results = listOf(
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
      cluster3.assertMergedTo(cluster1)
    }

    @Test
    @Disabled("this test is for CPR-623")
    fun `should merge 3 active cluster if matched records have exclude override markers between clusters`() {
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
        results = listOf(
          personB.matchId,
          personC.matchId,
          personD.matchId,
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
      cluster3.assertMergedTo(cluster1)
    }

    @Test
    fun `should not merge an active cluster to a matched cluster marked as needs attention`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey(UUIDStatusType.NEEDS_ATTENTION)
        .addPerson(personB)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        results = listOf(
          personB.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(1)
      cluster2.assertClusterIsOfSize(1)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)
    }

    @Test
    fun `should only merge active cluster to active clusters and exclude clusters marked as needs attention`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)

      val personB = createPerson(createRandomProbationPersonDetails())
      val cluster2 = createPersonKey(UUIDStatusType.NEEDS_ATTENTION)
        .addPerson(personB)

      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster3 = createPersonKey()
        .addPerson(personC)

      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        results = listOf(
          personB.matchId,
          personC.matchId,
        ),
      )

      reclusterService.recluster(cluster1, changedRecord = personA)

      cluster1.assertClusterIsOfSize(2)
      cluster2.assertClusterIsOfSize(1)
      cluster3.assertClusterIsOfSize(0)

      cluster1.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster2.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)
      cluster3.assertClusterStatus(UUIDStatusType.RECLUSTER_MERGE)

      cluster3.assertMergedTo(cluster1)
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
        results = listOf(
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
      val cluster3 = createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION)
        .addPerson(personE)

      stubClusterIsValid()
      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        results = listOf(
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
      cluster3.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)

      cluster2.assertMergedTo(cluster1)
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
        results = listOf(
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
        results = listOf(
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

  private fun PersonKeyEntity.assertClusterNotChanged(size: Int) {
    assertClusterStatus(UUIDStatusType.ACTIVE)
    assertClusterIsOfSize(size)
  }

  private fun PersonKeyEntity.assertClusterIsOfSize(size: Int) = awaitAssert { assertThat(personKeyRepository.findByPersonId(this.personId)?.personEntities?.size).isEqualTo(size) }

  private fun PersonKeyEntity.assertClusterStatus(status: UUIDStatusType) = awaitAssert { assertThat(personKeyRepository.findByPersonId(this.personId)?.status).isEqualTo(status) }

  private fun PersonKeyEntity.assertMergedTo(mergedCluster: PersonKeyEntity) {
    awaitAssert { assertThat(personKeyRepository.findByPersonId(this.personId)?.mergedTo).isEqualTo(mergedCluster.id) }
    checkTelemetry(
      TelemetryEventType.CPR_RECLUSTER_MERGE,
      mapOf(
        "FROM_UUID" to this.personId.toString(),
        "TO_UUID" to mergedCluster.personId.toString(),
      ),
    )
  }

  private fun createRandomProbationPersonDetails(): Person = Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn())))
}
