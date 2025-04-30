package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.NewUnmergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

class NewMergeServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var newUnmergeService: NewUnmergeService

  @Test
  fun `should unmerge 2 merged records that exist on same cluster`() {
    val cluster = createPersonKey()
    val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)

    val reactivatedRecord = createPerson(createRandomProbationPersonDetails())
    val mergedReactivatedRecord = mergeRecord(reactivatedRecord, unmergedRecord)

    stubPersonMatchScores()
    newUnmergeService.processUnmerge(mergedReactivatedRecord, unmergedRecord)

    checkTelemetry(TelemetryEventType.CPR_RECORD_UNMERGED, mapOf("FROM_SOURCE_SYSTEM_ID" to unmergedRecord.crn!!, "TO_SOURCE_SYSTEM_ID" to reactivatedRecord.crn!!))

    reactivatedRecord.assertNotMerged()

    reactivatedRecord.assertExcludedFrom(unmergedRecord)
    unmergedRecord.assertExcludedFrom(reactivatedRecord)

    cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    cluster.assertClusterIsOfSize(1)

    unmergedRecord.assertLinkedToCluster(cluster)
    reactivatedRecord.assertNotLinkedToCluster(cluster)
  }

  @Test
  fun `should unmerge 2 records that exist on same cluster but no merge link`() {
    val cluster = createPersonKey()
    val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)

    val reactivatedRecord = createPerson(createRandomProbationPersonDetails())

    stubPersonMatchScores()
    newUnmergeService.processUnmerge(reactivatedRecord, unmergedRecord)

    checkTelemetry(TelemetryEventType.CPR_RECORD_UNMERGED, mapOf("FROM_SOURCE_SYSTEM_ID" to unmergedRecord.crn!!, "TO_SOURCE_SYSTEM_ID" to reactivatedRecord.crn!!))

    reactivatedRecord.assertNotMerged()

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

  @Test
  fun `should unmerge 2 merged records that exist on same cluster and then link to another cluster`() {
    val cluster = createPersonKey()
    val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)

    val reactivatedRecord = createPerson(createRandomProbationPersonDetails())
    val mergedReactivatedRecord = mergeRecord(reactivatedRecord, unmergedRecord)

    val matchedPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

    stubOnePersonMatchHighConfidenceMatch(matchId = reactivatedRecord.matchId, matchedRecord = matchedPerson.matchId)
    newUnmergeService.processUnmerge(mergedReactivatedRecord, unmergedRecord)

    checkTelemetry(TelemetryEventType.CPR_RECORD_UNMERGED, mapOf("FROM_SOURCE_SYSTEM_ID" to unmergedRecord.crn!!, "TO_SOURCE_SYSTEM_ID" to reactivatedRecord.crn!!))

    reactivatedRecord.assertNotMerged()

    reactivatedRecord.assertExcludedFrom(unmergedRecord)
    unmergedRecord.assertExcludedFrom(reactivatedRecord)

    cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    cluster.assertClusterIsOfSize(1)

    unmergedRecord.assertLinkedToCluster(cluster)
    reactivatedRecord.assertLinkedToCluster(matchedPerson.personKey!!)

    matchedPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
    matchedPerson.personKey?.assertClusterIsOfSize(2)
  }

  @Nested
  inner class EventLog {

    @Test
    fun `should log unmerged event log`() {
      val cluster = createPersonKey()
      val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)

      val reactivatedRecord = createPerson(createRandomProbationPersonDetails())
      val mergedReactivatedRecord = mergeRecord(reactivatedRecord, unmergedRecord)

      stubPersonMatchScores()
      newUnmergeService.processUnmerge(mergedReactivatedRecord, unmergedRecord)

      checkEventLogExist(reactivatedRecord.crn!!, CPRLogEvents.CPR_UUID_CREATED)
      checkEventLog(reactivatedRecord.crn!!, CPRLogEvents.CPR_RECORD_UNMERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.uuid).isNotEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
        assertThat(eventLog.excludeOverrideMarkers).contains(unmergedRecord.id)
      }
      checkEventLog(unmergedRecord.crn!!, CPRLogEvents.CPR_RECORD_UPDATED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.uuid).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
        assertThat(eventLog.excludeOverrideMarkers).contains(reactivatedRecord.id)
      }
    }

    @Test
    fun `should should log needs attention when multiple records on cluster`() {
      val cluster = createPersonKey()
      val reactivatedRecord = createPerson(createRandomProbationPersonDetails(), cluster)
      val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)
      createPerson(createRandomProbationPersonDetails(), cluster)

      newUnmergeService.processUnmerge(reactivatedRecord, unmergedRecord)

      checkEventLog(unmergedRecord.crn!!, CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.uuid).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.NEEDS_ATTENTION)
      }
    }
  }
}
