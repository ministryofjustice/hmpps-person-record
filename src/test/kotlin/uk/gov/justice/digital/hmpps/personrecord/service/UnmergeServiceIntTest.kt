package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.UnmergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

class UnmergeServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var unmergeService: UnmergeService

  @Test
  fun `should unmerge 2 merged records that exist on same cluster`() {
    val cluster = createPersonKey()
    val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)

    val reactivatedRecord = createPerson(createRandomProbationPersonDetails())
    val mergedReactivatedRecord = mergeRecord(reactivatedRecord, unmergedRecord)

    stubPersonMatchScores()
    unmergeService.processUnmerge(mergedReactivatedRecord, unmergedRecord)

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
    unmergeService.processUnmerge(reactivatedRecord, unmergedRecord)

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
  fun `should unmerge 2 merged records that exist on same cluster and then link to another cluster`() {
    val cluster = createPersonKey()
    val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)

    val reactivatedRecord = createPerson(createRandomProbationPersonDetails())
    val mergedReactivatedRecord = mergeRecord(reactivatedRecord, unmergedRecord)

    val matchedPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

    stubOnePersonMatchHighConfidenceMatch(matchId = reactivatedRecord.matchId, matchedRecord = matchedPerson.matchId)
    unmergeService.processUnmerge(mergedReactivatedRecord, unmergedRecord)

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
    fun `should should log needs attention when multiple records on cluster`() {
      val cluster = createPersonKey()
      val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)
      createPerson(createRandomProbationPersonDetails(), cluster)

      val reactivatedRecord = createPerson(createRandomProbationPersonDetails())

      val mergedReactivatedRecord = mergeRecord(reactivatedRecord, unmergedRecord)

      stubPersonMatchScores()
      unmergeService.processUnmerge(mergedReactivatedRecord, unmergedRecord)

      checkEventLog(unmergedRecord.crn!!, CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.NEEDS_ATTENTION)
      }
    }
  }
}
