package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.message.NewUnmergeService

class NewMergeServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var newUnmergeService: NewUnmergeService

  @Test
  fun `should unmerge 2 merged records that exist on same cluster`() {
    val cluster = createPersonKey()
    val reactivatedRecord = createPerson(createRandomProbationPersonDetails(), cluster)
    val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)

    stubPersonMatchScores()

    newUnmergeService.processUnmerge(reactivatedRecord, unmergedRecord)

    reactivatedRecord.assertExcludedFrom(unmergedRecord)
    unmergedRecord.assertExcludedFrom(reactivatedRecord)

    cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    cluster.assertClusterIsOfSize(1)

    unmergedRecord.assertLinkedToCluster(cluster)
    reactivatedRecord.assertNotLinkedToCluster(cluster)
  }

  @Test
  fun `should set to needs attention when extra records on the cluster`() {
    val cluster = createPersonKey()
    val reactivatedRecord = createPerson(createRandomProbationPersonDetails(), cluster)
    val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)
    createPerson(createRandomProbationPersonDetails(), cluster)

    newUnmergeService.processUnmerge(reactivatedRecord, unmergedRecord)

    cluster.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)
    cluster.assertClusterIsOfSize(3)
  }

  private fun PersonEntity.assertNotLinkedToCluster(cluster: PersonKeyEntity) = awaitAssert {
    assertThat(personRepository.findByMatchId(this.matchId)?.personKey?.personUUID).isNotEqualTo(cluster.personUUID)
  }

  private fun PersonEntity.assertLinkedToCluster(cluster: PersonKeyEntity) = awaitAssert {
    assertThat(personRepository.findByMatchId(this.matchId)?.personKey?.personUUID).isEqualTo(cluster.personUUID)
  }

  private fun PersonEntity.assertExcludedFrom(personEntity: PersonEntity) = awaitAssert {
    assertThat(
      personRepository.findByMatchId(this.matchId)?.overrideMarkers?.filter { it.markerType == OverrideMarkerType.EXCLUDE && it.markerValue == personEntity.id },
    ).hasSize(1)
  }
}
