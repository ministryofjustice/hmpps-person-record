package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonUnmergedInfo
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECORD_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

@ExtendWith(OutputCaptureExtension::class)
class ProbationUnmergeEventListenerIntTest : MessagingTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
    }

    @Test
    fun `should retry on 500 error`() {
      stubPersonMatchUpsert()
      stubDeletePersonMatch()

      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      val unmergedPerson = createPersonWithNewKey(createRandomProbationPersonDetails(unmergedCrn))
      val reactivatedPerson = createPersonWithNewKey(createRandomProbationPersonDetails(reactivatedCrn))

      probationMergeEventAndResponseSetup(reactivatedCrn, unmergedCrn)
      checkEventLogExist(reactivatedCrn, CPRLogEvents.CPR_RECORD_MERGED)

      stub5xxResponse(probationUrl(unmergedCrn), "next request will succeed", "retry")

      probationUnmergeEventAndResponseSetup(
        reactivatedCrn,
        unmergedCrn,
        scenario = "retry",
        currentScenarioState = "next request will succeed",
        nextScenarioState = "next request will succeed",
      )

      expectNoMessagesOnQueueOrDlq(probationMergeEventsQueue)

      checkEventLogExist(reactivatedCrn, CPR_RECORD_UNMERGED)

      reactivatedPerson.assertHasLinkToCluster()
      reactivatedPerson.assertNotLinkedToCluster(unmergedPerson.personKey!!)
      reactivatedPerson.assertExcluded(unmergedPerson)

      unmergedPerson.assertHasLinkToCluster()
      unmergedPerson.assertExcluded(reactivatedPerson)

      unmergedPerson.assertHasOverrideMarker()
      reactivatedPerson.assertHasOverrideMarker()
      unmergedPerson.assertHasDifferentOverrideMarker(reactivatedPerson)
      unmergedPerson.assertHasSameOverrideScope(reactivatedPerson)
    }

    @Test
    fun `should unmerge 2 records from existing one record`() {
      val unmergedRecord = createPersonWithNewKey(createRandomProbationPersonDetails())

      val firstReactivatedRecord = createMergedPerson(createRandomProbationPersonDetails(), unmergedRecord.id)
      val secondReactivatedRecord = createMergedPerson(createRandomProbationPersonDetails(), unmergedRecord.id)

      probationUnmergeEventAndResponseSetup(firstReactivatedRecord.crn!!, unmergedRecord.crn!!)

      checkEventLogExist(firstReactivatedRecord.crn!!, CPR_RECORD_UNMERGED)

      probationUnmergeEventAndResponseSetup(secondReactivatedRecord.crn!!, unmergedRecord.crn!!)

      checkEventLogExist(secondReactivatedRecord.crn!!, CPR_RECORD_UNMERGED)

      firstReactivatedRecord.assertNotMerged()
      secondReactivatedRecord.assertNotMerged()

      firstReactivatedRecord.assertExcluded(unmergedRecord)
      secondReactivatedRecord.assertExcluded(unmergedRecord)
      unmergedRecord.assertExcluded(firstReactivatedRecord)

      val cluster = unmergedRecord.personKey!!
      cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster.assertClusterIsOfSize(1)

      unmergedRecord.assertLinkedToCluster(cluster)
      firstReactivatedRecord.assertNotLinkedToCluster(cluster)
      secondReactivatedRecord.assertNotLinkedToCluster(cluster)

      unmergedRecord.assertHasOverrideMarker()
      firstReactivatedRecord.assertHasOverrideMarker()
      secondReactivatedRecord.assertHasOverrideMarker()

      unmergedRecord.assertHasDifferentOverrideMarker(firstReactivatedRecord)
      unmergedRecord.assertHasDifferentOverrideMarker(secondReactivatedRecord)

      firstReactivatedRecord.assertHasDifferentOverrideMarker(secondReactivatedRecord)

      unmergedRecord.assertHasSameOverrideScope(firstReactivatedRecord)
      unmergedRecord.assertHasSameOverrideScope(secondReactivatedRecord)
      firstReactivatedRecord.assertHasDifferentOverrideScope(secondReactivatedRecord)

      unmergedRecord.assertOverrideScopeSize(2)
    }

    @Test
    fun `should not overwrite existing override marker`() {
      val unmergedRecord = createPersonWithNewKey(createRandomProbationPersonDetails())

      val firstReactivatedRecord = createMergedPerson(createRandomProbationPersonDetails(), unmergedRecord.id)
      val secondReactivatedRecord = createMergedPerson(createRandomProbationPersonDetails(), unmergedRecord.id)

      probationUnmergeEventAndResponseSetup(firstReactivatedRecord.crn!!, unmergedRecord.crn!!)
      checkEventLogExist(firstReactivatedRecord.crn!!, CPR_RECORD_UNMERGED)

      val initialOverrideMarker = awaitNotNull { personRepository.findByCrn(unmergedRecord.crn!!) }.overrideMarker
      assertThat(initialOverrideMarker).isNotNull()

      probationUnmergeEventAndResponseSetup(secondReactivatedRecord.crn!!, unmergedRecord.crn!!)
      checkEventLogExist(secondReactivatedRecord.crn!!, CPR_RECORD_UNMERGED)

      val finalOverrideMarker = awaitNotNull { personRepository.findByCrn(unmergedRecord.crn!!) }.overrideMarker
      assertThat(initialOverrideMarker).isEqualTo(finalOverrideMarker)
    }
  }

  @Nested
  inner class ErrorHandling {

    @Test
    fun `should put message on dlq when message processing fails`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()
      stub5xxResponse(probationUrl(unmergedCrn), "next request will fail", "failure")
      stub5xxResponse(probationUrl(unmergedCrn), "next request will fail", "failure", "next request will fail")
      stub5xxResponse(probationUrl(unmergedCrn), "next request will fail", "failure", "next request will fail")

      publishDomainEvent(
        ProbationPersonUnmerged(
          additionalInformation = ProbationPersonUnmergedInfo(
            reactivatedCrn = reactivatedCrn,
            unmergedCrn = unmergedCrn,
          ),
        ),
      )

      expectOneMessageOnDlq(probationMergeEventsQueue)
    }

    @Test
    fun `should discard if a 404 is received after retrying`(output: CapturedOutput) {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()
      stub404Response(probationUrl(unmergedCrn))

      publishDomainEvent(
        ProbationPersonUnmerged(
          additionalInformation = ProbationPersonUnmergedInfo(
            reactivatedCrn = reactivatedCrn,
            unmergedCrn = unmergedCrn,
          ),
        ),
      )
      expectNoMessagesOnQueueOrDlq(probationMergeEventsQueue)
      awaitAssert { assertThat(output.all).contains("Discarding message of type probation-case.unmerge.completed due to discardable not found exception") }
    }
  }
}
