package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import kotlin.jvm.optionals.getOrNull

class ProbationMergeEventListenerE2ETest : E2ETestBase() {

  @Test
  fun `process offender merge event with source record having a review`() {
    val targetCrn = randomCrn()
    val sourceCrn = randomCrn()
    val sourcePerson = createPersonWithNewKey(createRandomProbationPersonDetails(sourceCrn))
    val targetPersonDetails = createRandomProbationCase(targetCrn)
    val targetPerson = createPersonWithNewKey(Person.from(targetPersonDetails))

    // 1. merge the records
    probationMergeEventAndResponseSetup(
      OFFENDER_MERGED,
      sourceCrn = sourcePerson.crn!!,
      targetCrn = targetPerson.crn!!,
    )
    sourcePerson.assertMergedTo(targetPerson)

    // 2. unmerge the records
    val targetUnmergeSetup = ApiResponseSetup.from(targetPersonDetails).copy(crn = sourceCrn)
    probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, sourceCrn, targetCrn, reactivatedSetup = targetUnmergeSetup)
    sourcePerson.assertNotMerged()

    // 3. create new person with same target details - should get added to cluster
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, targetUnmergeSetup.copy(crn = randomCrn()))
    targetPerson.personKey!!.getReview().hasReviewSize(1)

    // TODO: merge again
  }

  @Test
  fun `processes offender merge event with records on same cluster`() {
    val sourceMasterDefendantId = randomDefendantId()
    val targetMasterDefendantId = randomDefendantId()
    val sourcePersonDetails = createRandomProbationPersonDetails()
    sourcePersonDetails.masterDefendantId = sourceMasterDefendantId
    val targetPersonDetails = createRandomProbationPersonDetails()
    targetPersonDetails.masterDefendantId = targetMasterDefendantId

    val sourcePerson = createPerson(sourcePersonDetails)
    val targetPerson = createPerson(targetPersonDetails)
    val sourceCrn = sourcePerson.crn!!
    val targetCrn = targetPerson.crn!!
    val cluster = createPersonKey()
      .addPerson(sourcePerson)
      .addPerson(targetPerson)

    probationMergeEventAndResponseSetup(
      OFFENDER_MERGED,
      sourceCrn = sourceCrn,
      targetCrn = targetCrn,
    )

    sourcePerson.assertMergedTo(targetPerson)
    sourcePerson.assertNotLinkedToCluster()

    cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    cluster.assertClusterIsOfSize(1)

    checkTelemetry(
      CPR_RECORD_MERGED,
      mapOf(
        "FROM_SOURCE_SYSTEM_ID" to sourceCrn,
        "TO_SOURCE_SYSTEM_ID" to targetCrn,
        "SOURCE_SYSTEM" to "DELIUS",
      ),
    )
    checkEventLog(sourceCrn, CPRLogEvents.CPR_RECORD_MERGED) { eventLogs ->
      assertThat(eventLogs).hasSize(1)
      assertThat(eventLogs.first().recordMergedTo).isEqualTo(targetPerson.id)
      assertThat(eventLogs.first().personUUID).isEqualTo(cluster.personUUID)
    }

    val mergedSource = awaitNotNull { personRepository.findByCrn(sourceCrn) }
    assertThat(mergedSource.masterDefendantId).isEqualTo(sourceMasterDefendantId)

    val mergedTarget = awaitNotNull { personRepository.findByCrn(targetCrn) }
    assertThat(mergedTarget.masterDefendantId).isEqualTo(targetMasterDefendantId)
  }

  @Test
  fun `processes offender merge event with records on different cluster`() {
    val sourcePersonDetails = createRandomProbationPersonDetails()
    val targetPersonDetails = createRandomProbationPersonDetails()

    val sourcePerson = createPerson(sourcePersonDetails)
    val targetPerson = createPerson(targetPersonDetails)
    val sourceCrn = sourcePerson.crn!!
    val targetCrn = targetPerson.crn!!
    val sourceCluster = createPersonKey()
      .addPerson(sourcePerson)
    val targetCluster = createPersonKey()
      .addPerson(targetPerson)

    probationMergeEventAndResponseSetup(
      OFFENDER_MERGED,
      sourceCrn = sourceCrn,
      targetCrn = targetCrn,
    )

    sourcePerson.assertMergedTo(targetPerson)
    sourcePerson.assertNotLinkedToCluster()

    targetCluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    targetCluster.assertClusterIsOfSize(1)

    // expect cluster to be deleted because no records exist after merge
    val sourceClusterPostMerge = personKeyRepository.findById(sourceCluster.id!!).getOrNull()
    assertThat(sourceClusterPostMerge).isNull()

    checkTelemetry(
      CPR_RECORD_MERGED,
      mapOf(
        "FROM_SOURCE_SYSTEM_ID" to sourceCrn,
        "TO_SOURCE_SYSTEM_ID" to targetCrn,
        "SOURCE_SYSTEM" to "DELIUS",
      ),
    )
    checkEventLog(sourceCrn, CPRLogEvents.CPR_RECORD_MERGED) { eventLogs ->
      assertThat(eventLogs).hasSize(1)
      assertThat(eventLogs.first().recordMergedTo).isEqualTo(targetPerson.id)
      assertThat(eventLogs.first().personUUID).isEqualTo(sourceCluster.personUUID)
    }
  }
}
