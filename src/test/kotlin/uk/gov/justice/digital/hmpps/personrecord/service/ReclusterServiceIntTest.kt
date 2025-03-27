package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.personrecord.service.message.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_NO_CHANGE
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class ReclusterServiceIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  private lateinit var reclusterService: ReclusterService

  @Nested
  inner class NoChangeToCluster {

    @Test
    fun `should do nothing when match return same items from cluster with no records`() {
      val personA = createPerson(createRandomPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)

      stubNoMatchesPersonMatch(matchId = personA.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_NO_CHANGE,
        mapOf("UUID" to cluster.personId.toString()),
      )
    }

    @Test
    fun `should do nothing when match return same items from cluster with multiple records`() {
      val personA = createPerson(createRandomPersonDetails())
      val personB = createPerson(createRandomPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      stubOnePersonMatchHighConfidenceMatch(matchId = personA.matchId, matchedRecord = personB.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_NO_CHANGE,
        mapOf("UUID" to cluster.personId.toString()),
      )
    }

  }

  @Nested
  inner class FewerHighConfidenceMatchesThanInExistingCluster {

    @BeforeEach
    fun beforeEach() {
      telemetryRepository.deleteAll()
    }

    @Test
    fun `should mark as need attention when only matches one in cluster with multiple records`() {
      val personA = createPerson(createRandomPersonDetails())
      val personB = createPerson(createRandomPersonDetails())
      val personC = createPerson(createRandomPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)
        .addPerson(personC)

      stubOnePersonMatchHighConfidenceMatch(matchId = personA.matchId, matchedRecord = personB.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to cluster.personId.toString()),
      )
      clusterIsSetToNeedAttention(cluster)
    }

    @Test
    fun `should mark as need attention when matches no record in cluster with 2 records`() {
      val personA = createPerson(createRandomPersonDetails())
      val personB = createPerson(createRandomPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      stubNoMatchesPersonMatch(matchId = personA.matchId)

      reclusterService.recluster(cluster, changedRecord = personA)

      checkTelemetry(
        CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        mapOf("UUID" to cluster.personId.toString()),
      )
      clusterIsSetToNeedAttention(cluster)
    }

    @Test
    fun `should mark as need attention when matches no record in cluster with multiple records`() {
      val personA = createPerson(createRandomPersonDetails())
      val personB = createPerson(createRandomPersonDetails())
      val personC = createPerson(createRandomPersonDetails())
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
      clusterIsSetToNeedAttention(cluster)
    }

  }

  private fun clusterIsSetToNeedAttention(cluster: PersonKeyEntity) = awaitAssert { assertThat(personKeyRepository.findByPersonId(cluster.personId)?.status).isEqualTo(UUIDStatusType.NEEDS_ATTENTION) }

  private fun createRandomPersonDetails(): Person = Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn())))

}
