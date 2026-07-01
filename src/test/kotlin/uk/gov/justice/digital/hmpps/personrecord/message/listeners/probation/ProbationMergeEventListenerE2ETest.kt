package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_DELETION
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
    val targetPersonDetails = createRandomProbationCase(targetCrn)
    val sourcePerson = createPersonWithNewKey(Person.from(targetPersonDetails.copy(identifiers = targetPersonDetails.identifiers.copy(crn = sourceCrn))))
    val targetPerson = createPersonWithNewKey(Person.from(targetPersonDetails))

    // 1. merge the records
    probationMergeEventAndResponseSetup(
      sourceCrn = sourcePerson.crn!!,
      targetCrn = targetPerson.crn!!,
      apiResponseSetup = ApiResponseSetup.from(targetPersonDetails),
    )
    sourcePerson.assertMergedTo(targetPerson)

    // 2. unmerge the records
    val targetUnmergeSetup = ApiResponseSetup.from(targetPersonDetails).copy(crn = sourceCrn)
    probationUnmergeEventAndResponseSetup(sourceCrn, targetCrn, reactivatedSetup = targetUnmergeSetup, unmergedSetup = targetUnmergeSetup.copy(crn = sourceCrn))
    sourcePerson.assertNotMerged()

    // 3. create new person with same target details - should match both
    val personThreeCrn = randomCrn()
    probationCreateEventAndResponseSetup(targetUnmergeSetup.copy(crn = personThreeCrn))
    val review = targetPerson.personKey!!.getReviews().first()
    review.assertReviewSize(3)

    // 4. merge again - should delete review
    probationMergeEventAndResponseSetup(
      sourceCrn = sourcePerson.crn!!,
      targetCrn = targetPerson.crn!!,
      apiResponseSetup = ApiResponseSetup.from(targetPersonDetails),
    )
    sourcePerson.assertMergedTo(targetPerson)
    sourcePerson.personKey!!.assertPersonKeyDeleted()
    review.assertRemoved()
  }

  @Test
  fun `should delete record when cluster is part of a review`() {
    val targetCrn = randomCrn()
    val sourceCrn = randomCrn()
    val targetPersonDetails = createRandomProbationCase(targetCrn)
    val sourcePerson = createPersonWithNewKey(Person.from(targetPersonDetails.copy(identifiers = targetPersonDetails.identifiers.copy(crn = sourceCrn))))
    val targetPerson = createPersonWithNewKey(Person.from(targetPersonDetails))

    // 1. merge the records
    probationMergeEventAndResponseSetup(
      sourceCrn = sourcePerson.crn!!,
      targetCrn = targetPerson.crn!!,
      apiResponseSetup = ApiResponseSetup.from(targetPersonDetails),
    )
    sourcePerson.assertMergedTo(targetPerson)

    // 2. unmerge the records
    val targetUnmergeSetup = ApiResponseSetup.from(targetPersonDetails).copy(crn = sourceCrn)
    probationUnmergeEventAndResponseSetup(sourceCrn, targetCrn, reactivatedSetup = targetUnmergeSetup, unmergedSetup = targetUnmergeSetup.copy(crn = sourceCrn))
    sourcePerson.assertNotMerged()

    // 3. create new person with same target details - should match both
    probationCreateEventAndResponseSetup(targetUnmergeSetup.copy(crn = randomCrn()))
    val review = targetPerson.personKey!!.getReviews().first()
    review.assertReviewSize(3)

    // 4. delete record
    publishProbationPersonDeletedEvent(PROBATION_PERSON_DELETION, sourceCrn)
    sourcePerson.assertPersonDeleted()
    review.assertRemoved()
  }

  @Test
  fun `should delete cluster when reclustering a single record cluster is part of a review`() {
    val targetCrn = randomCrn()
    val sourceCrn = randomCrn()
    val targetPersonDetails = createRandomProbationCase(targetCrn)
    val sourcePerson = createPersonWithNewKey(Person.from(targetPersonDetails.copy(identifiers = targetPersonDetails.identifiers.copy(crn = sourceCrn))))
    val targetPerson = createPersonWithNewKey(Person.from(targetPersonDetails))

    // 1. merge the target and source records
    probationMergeEventAndResponseSetup(
      sourceCrn = sourcePerson.crn!!,
      targetCrn = targetPerson.crn!!,
      apiResponseSetup = ApiResponseSetup.from(targetPersonDetails),
    )
    sourcePerson.assertMergedTo(targetPerson)

    // 2. unmerge the target and source records
    val targetUnmergeSetup = ApiResponseSetup.from(targetPersonDetails).copy(crn = sourceCrn)
    probationUnmergeEventAndResponseSetup(
      sourceCrn,
      targetCrn,
      reactivatedSetup = targetUnmergeSetup,
      unmergedSetup = targetUnmergeSetup.copy(crn = sourceCrn),
    )
    sourcePerson.assertNotMerged()

    // 3. create person 3 to match both target and source records
    probationCreateEventAndResponseSetup(targetUnmergeSetup.copy(crn = randomCrn()))
    val review = targetPerson.personKey!!.getReviews().first()
    review.assertReviewSize(3)

    // 4. create person 4 that matches no other records
    val fourthPerson = randomCrn()
    probationCreateEventAndResponseSetup(ApiResponseSetup.from(createRandomProbationCase(fourthPerson)))

    // 5. make the target record no longer match the source record or person 3
    val matchTargetAndFourthPersonSetup = ApiResponseSetup.from(createRandomProbationCase(fourthPerson))
    probationUpdateEventAndResponseSetup(matchTargetAndFourthPersonSetup.copy(crn = targetCrn))
    targetPerson.personKey!!.assertClusterIsOfSize(1)

    // 6. make person 4 match the target record - recluster should delete the target cluster and delete the linked review
    probationUpdateEventAndResponseSetup(matchTargetAndFourthPersonSetup.copy(crn = fourthPerson))
    targetPerson.personKey!!.assertPersonKeyDeleted()
    review.assertRemoved()
  }

  @Test
  fun `when a cluster has multiple reviews, all are deleted when the cluster is deleted`() {
    val targetCrn = randomCrn()
    val sourceCrn = randomCrn()
    val targetPersonDetails = createRandomProbationCase(targetCrn)
    val sourcePerson = createPersonWithNewKey(Person.from(targetPersonDetails.copy(identifiers = targetPersonDetails.identifiers.copy(crn = sourceCrn))))
    val targetPerson = createPersonWithNewKey(Person.from(targetPersonDetails))

    // 1. merge the target and source records
    probationMergeEventAndResponseSetup(
      sourceCrn = sourcePerson.crn!!,
      targetCrn = targetPerson.crn!!,
      apiResponseSetup = ApiResponseSetup.from(targetPersonDetails),
    )
    sourcePerson.assertMergedTo(targetPerson)

    // 2. create person 3 to match target and join its cluster
    val person3Crn = randomCrn()
    probationCreateEventAndResponseSetup(ApiResponseSetup.from(targetPersonDetails).copy(crn = person3Crn))
    val targetCluster = targetPerson.personKey
    targetCluster?.assertClusterIsOfSize(2)

    // 3. change person 3 to not match target so cluster goes into review
    probationUpdateEventAndResponseSetup(ApiResponseSetup.from(createRandomProbationCase(crn = person3Crn)))
    val review = targetCluster!!.getReviews().first()
    review.assertReviewSize(1)

    // 4. unmerge the target and source records
    val targetUnmergeSetup = ApiResponseSetup.from(targetPersonDetails).copy(crn = sourceCrn)
    probationUnmergeEventAndResponseSetup(
      sourceCrn,
      targetCrn,
      reactivatedSetup = targetUnmergeSetup,
      unmergedSetup = targetUnmergeSetup.copy(crn = sourceCrn),
    )
    sourcePerson.assertNotMerged()

    //  5. create a person who matches both, it creates a 2nd review
    val person5Crn = randomCrn()
    probationCreateEventAndResponseSetup(ApiResponseSetup.from(targetPersonDetails).copy(crn = person5Crn))
    assertThat(targetCluster.getReviews()).hasSize(2)

    // 6. delete the cluster with the target person by deleting both records on it
    publishProbationPersonDeletedEvent(PROBATION_PERSON_DELETION, targetCrn)
    targetPerson.assertPersonDeleted()
    publishProbationPersonDeletedEvent(PROBATION_PERSON_DELETION, person3Crn)
    targetCluster.assertPersonKeyDeleted()
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
