package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.NewMergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

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
    val fromUUID = from.personKey?.personUUID.toString()
    val toUUID = to.personKey?.personUUID.toString()

    newMergeService.processMerge(from, to)

    from.personKey?.assertClusterStatus(UUIDStatusType.MERGED)
    from.personKey?.assertClusterIsOfSize(0)
    from.personKey?.assertMergedTo(to.personKey!!)

    to.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
    to.personKey?.assertClusterIsOfSize(1)

    checkEventLog(from.crn!!, CPRLogEvents.CPR_UUID_MERGED) { eventLogs ->
      assertThat(eventLogs).hasSize(1)
      val event = eventLogs.first()
      assertThat(event.uuid.toString()).isEqualTo(fromUUID)
      assertThat(event.uuidStatusType).isEqualTo(UUIDStatusType.MERGED)
    }
    checkEventLogExist(from.crn!!, CPRLogEvents.CPR_RECORD_MERGED)

    checkTelemetry(
      TelemetryEventType.CPR_UUID_MERGED,
      mapOf(
        "FROM_UUID" to fromUUID,
        "TO_UUID" to toUUID,
        "SOURCE_SYSTEM" to SourceSystemType.DELIUS.name,
      ),
    )
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
  fun `should merge records on same UUIDs`() {
    val cluster = createPersonKey()
    val from = createPerson(createRandomProbationPersonDetails(), cluster)
    val to = createPerson(createRandomProbationPersonDetails(), cluster)

    newMergeService.processMerge(from, to)

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

    newMergeService.processMerge(from, to)

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
