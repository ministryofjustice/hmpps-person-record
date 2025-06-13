package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

class MergeServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var mergeService: MergeService

  @BeforeEach
  fun beforeEach() {
    stubDeletePersonMatch()
  }

  @Test
  fun `should merge records on same UUIDs`() {
    val cluster = createPersonKey()
    val from = createPerson(createRandomProbationPersonDetails(), cluster)
    val to = createPerson(createRandomProbationPersonDetails(), cluster)

    mergeService.processMerge(from, to)

    cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    cluster.assertClusterIsOfSize(1)

    from.assertMergedTo(to)
    from.assertNotLinkedToCluster()

    checkEventLogExist(from.crn!!, CPRLogEvents.CPR_RECORD_MERGED)
    checkTelemetry(
      TelemetryEventType.CPR_RECORD_MERGED,
      mapOf(
        "TO_SOURCE_SYSTEM_ID" to to.crn,
        "FROM_SOURCE_SYSTEM_ID" to from.crn,
        "SOURCE_SYSTEM" to SourceSystemType.DELIUS.name,
      ),
    )
  }

  @Test
  fun `should move record from cluster with multiple records without merging the whole cluster`() {
    val fromCluster = createPersonKey()
    val from = createPerson(createRandomProbationPersonDetails(), fromCluster)
    createPerson(createRandomProbationPersonDetails(), fromCluster)

    val toCluster = createPersonKey()
    val to = createPerson(createRandomProbationPersonDetails(), toCluster)
    createPerson(createRandomProbationPersonDetails(), toCluster)

    mergeService.processMerge(from, to)

    fromCluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    fromCluster.assertClusterIsOfSize(1)

    toCluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    toCluster.assertClusterIsOfSize(2)

    from.assertMergedTo(to)
    from.assertNotLinkedToCluster()

    checkEventLogExist(from.crn!!, CPRLogEvents.CPR_RECORD_MERGED)
    checkTelemetry(
      TelemetryEventType.CPR_RECORD_MERGED,
      mapOf(
        "TO_SOURCE_SYSTEM_ID" to to.crn,
        "FROM_SOURCE_SYSTEM_ID" to from.crn,
        "SOURCE_SYSTEM" to SourceSystemType.DELIUS.name,
      ),
    )
  }
}
