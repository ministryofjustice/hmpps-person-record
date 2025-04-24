package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.message.NewMergeService

class NewMergeServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var newMergeService: NewMergeService

  @BeforeEach
  fun beforeEach() {
    stubDeletePersonMatch()
  }

  @Test
  fun `should merge records on different UUIDs with single records`() {
    val from = createPersonWithNewKey(createRandomProbationPersonDetails())
    val to = createPersonWithNewKey(createRandomProbationPersonDetails())

    newMergeService.processMerge(from, to)

    from.personKey?.assertClusterStatus(UUIDStatusType.MERGED)
    from.personKey?.assertClusterIsOfSize(0)
    from.personKey?.assertMergedTo(to.personKey!!)

    to.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
    to.personKey?.assertClusterIsOfSize(1)
  }

  @Test
  fun `should merge records on same UUIDs`() {
    val cluster = createPersonKey()
    val from = createPerson(createRandomProbationPersonDetails(), cluster)
    val to = createPerson(createRandomProbationPersonDetails(), cluster)

    newMergeService.processMerge(from, to)

    cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    cluster.assertClusterIsOfSize(1)

    from.assertMergedTo(to)
    from.assertNotLinkedToCluster()
  }

  @Test
  fun `should move record from cluster with multiple records without merging the whole cluster`() {
    val fromCluster = createPersonKey()
    val from = createPerson(createRandomProbationPersonDetails(), fromCluster)
    createPerson(createRandomProbationPersonDetails(), fromCluster)

    val toCluster = createPersonKey()
    val to = createPerson(createRandomProbationPersonDetails(), toCluster)
    createPerson(createRandomProbationPersonDetails(), toCluster)

    newMergeService.processMerge(from, to)

    fromCluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    fromCluster.assertClusterIsOfSize(1)

    toCluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    toCluster.assertClusterIsOfSize(2)

    from.assertMergedTo(to)
    from.assertNotLinkedToCluster()
  }
}
