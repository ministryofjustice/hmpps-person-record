package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.UnmergeService

class UnmergeServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var unmergeService: UnmergeService

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
