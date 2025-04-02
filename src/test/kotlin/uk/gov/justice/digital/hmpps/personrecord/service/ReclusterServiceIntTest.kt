package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

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
      probationEventAndResponseSetup(OFFENDER_ALIAS_CHANGED, ApiResponseSetup(crn = newPersonCrn))

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
      probationEventAndResponseSetup(OFFENDER_ALIAS_CHANGED, ApiResponseSetup(crn = personA.crn))

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

    @Test
    fun `should do nothing when matches only one record in cluster with multiple records and links to other cluster but is still a valid cluster`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val personD = createPerson(createRandomProbationPersonDetails())
      createPersonKey()
        .addPerson(personD)

      stubClusterIsValid()
      stubXPersonMatchHighConfidenceMatches(
        matchId = personA.matchId,
        results = listOf(
          personB.matchId,
          personD.matchId,
        ),
      )

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
        mapOf("UUID" to cluster.personId.toString()),
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
        mapOf("UUID" to cluster.personId.toString()),
      )
      cluster.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)
    }

    @Test
    fun `should mark as need attention when matches less records in cluster and contains matches from another cluster`() {
      val personA = createPerson(createRandomProbationPersonDetails())
      val personB = createPerson(createRandomProbationPersonDetails())
      val personC = createPerson(createRandomProbationPersonDetails())
      val clusterA = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      val personD = createPerson(createRandomProbationPersonDetails())
      createPersonKey()
        .addPerson(personD)

      stubClusterIsNotValid()
      stubOnePersonMatchHighConfidenceMatch(matchId = personA.matchId, matchedRecord = personD.matchId)

      reclusterService.recluster(clusterA, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to clusterA.personId.toString()),
      )
      clusterA.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)
    }
  }

  private fun PersonKeyEntity.assertClusterNotChanged(size: Int) {
    assertClusterStatus(UUIDStatusType.ACTIVE)
    assertClusterIsOfSize(size)
  }

  private fun PersonKeyEntity.assertClusterIsOfSize(size: Int) = awaitAssert { assertThat(personKeyRepository.findByPersonId(this.personId)?.personEntities?.size).isEqualTo(size) }

  private fun PersonKeyEntity.assertClusterStatus(status: UUIDStatusType) = awaitAssert { assertThat(personKeyRepository.findByPersonId(this.personId)?.status).isEqualTo(status) }

  private fun createRandomProbationPersonDetails(): Person = Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn())))
}
