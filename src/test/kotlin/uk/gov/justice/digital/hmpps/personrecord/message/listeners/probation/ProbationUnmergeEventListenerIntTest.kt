package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class ProbationUnmergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
    }

    @Test
    fun `should unmerge 2 records that have been merged`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      val cluster = createPersonKey()
      val unmergedPerson = createPerson(createRandomProbationPersonDetails(unmergedCrn), cluster)
      val reactivatedPerson = createPerson(createRandomProbationPersonDetails(reactivatedCrn))

      mergeRecord(reactivatedPerson, unmergedPerson)

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivatedCrn, unmergedCrn)

      checkEventLogExist(reactivatedPerson.crn!!, CPRLogEvents.CPR_UUID_CREATED)
      checkEventLog(reactivatedPerson.crn!!, CPRLogEvents.CPR_RECORD_UNMERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isNotEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
        assertThat(eventLog.excludeOverrideMarkers).contains(unmergedPerson.id)
        assertThat(eventLog.overrideMarker).isNotNull()
        assertThat(eventLog.overrideScopes).isNotEmpty()
      }
      checkEventLog(unmergedPerson.crn!!, CPRLogEvents.CPR_RECORD_UPDATED) { eventLogs ->
        assertThat(eventLogs).hasSize(2)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
        assertThat(eventLog.excludeOverrideMarkers).contains(reactivatedPerson.id)
        assertThat(eventLog.overrideMarker).isNotNull()
        assertThat(eventLog.overrideScopes).isNotEmpty()
      }
      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_CREATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_CREATED,
        mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
        times = 0,
      )
      checkTelemetry(
        CPR_RECORD_UNMERGED,
        mapOf(
          "TO_SOURCE_SYSTEM_ID" to reactivatedCrn,
          "FROM_SOURCE_SYSTEM_ID" to unmergedCrn,
          "UNMERGED_UUID" to cluster.personUUID.toString(),
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )

      unmergedPerson.assertHasLinkToCluster()
      unmergedPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      unmergedPerson.personKey?.assertClusterIsOfSize(1)
      unmergedPerson.assertExcludedFrom(reactivatedPerson)

      reactivatedPerson.assertHasLinkToCluster()
      reactivatedPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      reactivatedPerson.personKey?.assertClusterIsOfSize(1)
      reactivatedPerson.assertNotLinkedToCluster(unmergedPerson.personKey!!)
      reactivatedPerson.assertExcludedFrom(unmergedPerson)
      reactivatedPerson.assertNotMerged()

      unmergedPerson.assertHasOverrideMarker()
      reactivatedPerson.assertHasOverrideMarker()
      unmergedPerson.assertOverrideScopeSize(1)
      reactivatedPerson.assertOverrideScopeSize(1)
      unmergedPerson.assertHasDifferentOverrideMarker(reactivatedPerson)
      unmergedPerson.assertHasSameOverrideScope(reactivatedPerson)
    }

    @Test
    fun `should create record when reactivated record not found and should create a UUID`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      val unmergedPerson = createPersonWithNewKey(createRandomProbationPersonDetails(unmergedCrn))

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivatedCrn, unmergedCrn)

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_CREATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UNMERGED,
        mapOf(
          "TO_SOURCE_SYSTEM_ID" to reactivatedCrn,
          "FROM_SOURCE_SYSTEM_ID" to unmergedCrn,
          "UNMERGED_UUID" to unmergedPerson.personKey?.personUUID.toString(),
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )

      val reactivatedPerson = awaitNotNullPerson { personRepository.findByCrn(reactivatedCrn) }
      reactivatedPerson.assertHasLinkToCluster()
      reactivatedPerson.assertNotLinkedToCluster(unmergedPerson.personKey!!)
      reactivatedPerson.assertExcludedFrom(unmergedPerson)
      reactivatedPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      reactivatedPerson.personKey?.assertClusterIsOfSize(1)

      unmergedPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      unmergedPerson.personKey?.assertClusterIsOfSize(1)
      unmergedPerson.assertNotLinkedToCluster(reactivatedPerson.personKey!!)
      unmergedPerson.assertExcludedFrom(reactivatedPerson)

      unmergedPerson.assertHasOverrideMarker()
      reactivatedPerson.assertHasOverrideMarker()
      unmergedPerson.assertHasDifferentOverrideMarker(reactivatedPerson)
      unmergedPerson.assertHasSameOverrideScope(reactivatedPerson)
    }

    @Test
    fun `should retry on 500 error`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      val unmergedPerson = createPersonWithNewKey(createRandomProbationPersonDetails(unmergedCrn))
      val reactivatedPerson = createPerson(createRandomProbationPersonDetails(reactivatedCrn))

      mergeRecord(reactivatedPerson, unmergedPerson)

      stub5xxResponse(probationUrl(unmergedCrn), "next request will succeed", "retry")

      probationUnmergeEventAndResponseSetup(
        OFFENDER_UNMERGED,
        reactivatedCrn,
        unmergedCrn,
        scenario = "retry",
        currentScenarioState = "next request will succeed",
        nextScenarioState = "next request will succeed",
      )

      expectNoMessagesOnQueueOrDlq(probationMergeEventsQueue)

      checkTelemetry(
        CPR_RECORD_UNMERGED,
        mapOf(
          "TO_SOURCE_SYSTEM_ID" to reactivatedCrn,
          "FROM_SOURCE_SYSTEM_ID" to unmergedCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )

      reactivatedPerson.assertHasLinkToCluster()
      reactivatedPerson.assertNotLinkedToCluster(unmergedPerson.personKey!!)
      reactivatedPerson.assertExcludedFrom(unmergedPerson)

      unmergedPerson.assertHasLinkToCluster()
      unmergedPerson.assertExcludedFrom(reactivatedPerson)

      unmergedPerson.assertHasOverrideMarker()
      reactivatedPerson.assertHasOverrideMarker()
      unmergedPerson.assertHasDifferentOverrideMarker(reactivatedPerson)
      unmergedPerson.assertHasSameOverrideScope(reactivatedPerson)
    }

    @Test
    fun `should create record when unmerged record not found`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      val reactivatedPerson = createPersonWithNewKey(createRandomProbationPersonDetails(reactivatedCrn))

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivatedCrn, unmergedCrn)

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_CREATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UNMERGED,
        mapOf(
          "TO_SOURCE_SYSTEM_ID" to reactivatedCrn,
          "FROM_SOURCE_SYSTEM_ID" to unmergedCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      val unmergedPerson = awaitNotNullPerson { personRepository.findByCrn(unmergedCrn) }
      unmergedPerson.assertHasLinkToCluster()
      unmergedPerson.assertExcludedFrom(reactivatedPerson)
      unmergedPerson.assertNotLinkedToCluster(reactivatedPerson.personKey!!)

      reactivatedPerson.assertHasLinkToCluster()
      reactivatedPerson.assertNotLinkedToCluster(unmergedPerson.personKey!!)
      reactivatedPerson.assertExcludedFrom(unmergedPerson)

      unmergedPerson.assertHasOverrideMarker()
      reactivatedPerson.assertHasOverrideMarker()
      unmergedPerson.assertHasDifferentOverrideMarker(reactivatedPerson)
      unmergedPerson.assertHasSameOverrideScope(reactivatedPerson)
    }

    @Test
    fun `should unmerge 2 merged records that exist on same cluster`() {
      val cluster = createPersonKey()
      val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)

      val reactivatedRecord = createPerson(createRandomProbationPersonDetails())
      val mergedReactivatedRecord = mergeRecord(reactivatedRecord, unmergedRecord)

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, mergedReactivatedRecord.crn!!, unmergedRecord.crn!!)

      checkTelemetry(CPR_RECORD_UNMERGED, mapOf("FROM_SOURCE_SYSTEM_ID" to unmergedRecord.crn!!, "TO_SOURCE_SYSTEM_ID" to reactivatedRecord.crn!!))

      reactivatedRecord.assertNotMerged()

      reactivatedRecord.assertExcludedFrom(unmergedRecord)
      unmergedRecord.assertExcludedFrom(reactivatedRecord)

      cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster.assertClusterIsOfSize(1)

      unmergedRecord.assertLinkedToCluster(cluster)
      reactivatedRecord.assertNotLinkedToCluster(cluster)

      unmergedRecord.assertHasOverrideMarker()
      reactivatedRecord.assertHasOverrideMarker()
      unmergedRecord.assertHasDifferentOverrideMarker(reactivatedRecord)
      unmergedRecord.assertHasSameOverrideScope(reactivatedRecord)
    }

    @Test
    fun `should unmerge 2 records that exist on same cluster but no merge link`() {
      val cluster = createPersonKey()
      val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)

      val reactivatedRecord = createPerson(createRandomProbationPersonDetails())

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivatedRecord.crn!!, unmergedRecord.crn!!)

      checkTelemetry(CPR_RECORD_UNMERGED, mapOf("FROM_SOURCE_SYSTEM_ID" to unmergedRecord.crn!!, "TO_SOURCE_SYSTEM_ID" to reactivatedRecord.crn!!))

      reactivatedRecord.assertNotMerged()

      reactivatedRecord.assertExcludedFrom(unmergedRecord)
      unmergedRecord.assertExcludedFrom(reactivatedRecord)

      cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster.assertClusterIsOfSize(1)

      unmergedRecord.assertLinkedToCluster(cluster)
      reactivatedRecord.assertNotLinkedToCluster(cluster)

      unmergedRecord.assertHasOverrideMarker()
      reactivatedRecord.assertHasOverrideMarker()
      unmergedRecord.assertHasDifferentOverrideMarker(reactivatedRecord)
      unmergedRecord.assertHasSameOverrideScope(reactivatedRecord)
    }

    @Test
    fun `should unmerge 2 merged records that exist on same cluster and then link to another cluster`() {
      val cluster = createPersonKey()
      val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)

      val reactivatedRecord = createPerson(createRandomProbationPersonDetails())
      val mergedReactivatedRecord = mergeRecord(reactivatedRecord, unmergedRecord)

      val matchedPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

      stubOnePersonMatchAboveJoinThreshold(matchId = reactivatedRecord.matchId, matchedRecord = matchedPerson.matchId)
      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, mergedReactivatedRecord.crn!!, unmergedRecord.crn!!)

      checkTelemetry(CPR_RECORD_UNMERGED, mapOf("FROM_SOURCE_SYSTEM_ID" to unmergedRecord.crn!!, "TO_SOURCE_SYSTEM_ID" to reactivatedRecord.crn!!))
      checkEventLog(unmergedRecord.crn!!, CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION) { eventLogs ->
        assertThat(eventLogs).hasSize(0)
      }
      reactivatedRecord.assertNotMerged()

      reactivatedRecord.assertExcludedFrom(unmergedRecord)
      unmergedRecord.assertExcludedFrom(reactivatedRecord)

      cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster.assertClusterIsOfSize(1)

      unmergedRecord.assertLinkedToCluster(cluster)
      reactivatedRecord.assertLinkedToCluster(matchedPerson.personKey!!)

      matchedPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      matchedPerson.personKey?.assertClusterIsOfSize(2)

      matchedPerson.assertDoesNotHaveOverrideMarker()
      unmergedRecord.assertHasOverrideMarker()
      reactivatedRecord.assertHasOverrideMarker()

      unmergedRecord.assertHasDifferentOverrideMarker(reactivatedRecord)
      unmergedRecord.assertHasSameOverrideScope(reactivatedRecord)
    }

    @Test
    fun `should unmerge 2 records from existing one record`() {
      val cluster = createPersonKey()
      val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)

      val firstReactivatedRecord = createPerson(createRandomProbationPersonDetails())
      val secondReactivatedRecord = createPerson(createRandomProbationPersonDetails())

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, firstReactivatedRecord.crn!!, unmergedRecord.crn!!)

      checkTelemetry(CPR_RECORD_UNMERGED, mapOf("FROM_SOURCE_SYSTEM_ID" to unmergedRecord.crn!!, "TO_SOURCE_SYSTEM_ID" to firstReactivatedRecord.crn!!))

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, secondReactivatedRecord.crn!!, unmergedRecord.crn!!)

      checkTelemetry(CPR_RECORD_UNMERGED, mapOf("FROM_SOURCE_SYSTEM_ID" to unmergedRecord.crn!!, "TO_SOURCE_SYSTEM_ID" to secondReactivatedRecord.crn!!))

      firstReactivatedRecord.assertNotMerged()
      secondReactivatedRecord.assertNotMerged()

      firstReactivatedRecord.assertExcludedFrom(unmergedRecord)
      secondReactivatedRecord.assertExcludedFrom(unmergedRecord)
      unmergedRecord.assertExcludedFrom(firstReactivatedRecord)

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
      val cluster = createPersonKey()
      val unmergedRecord = createPerson(createRandomProbationPersonDetails(), cluster)
      val firstReactivatedRecord = createPerson(createRandomProbationPersonDetails())
      val secondReactivatedRecord = createPerson(createRandomProbationPersonDetails())

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, firstReactivatedRecord.crn!!, unmergedRecord.crn!!)
      checkTelemetry(CPR_RECORD_UNMERGED, mapOf("FROM_SOURCE_SYSTEM_ID" to unmergedRecord.crn!!, "TO_SOURCE_SYSTEM_ID" to firstReactivatedRecord.crn!!))

      val initialOverrideMarker = awaitNotNullPerson { personRepository.findByCrn(unmergedRecord.crn!!) }.overrideMarker
      assertThat(initialOverrideMarker).isNotNull()

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, secondReactivatedRecord.crn!!, unmergedRecord.crn!!)
      checkTelemetry(CPR_RECORD_UNMERGED, mapOf("FROM_SOURCE_SYSTEM_ID" to unmergedRecord.crn!!, "TO_SOURCE_SYSTEM_ID" to secondReactivatedRecord.crn!!))

      val finalOverrideMarker = awaitNotNullPerson { personRepository.findByCrn(unmergedRecord.crn!!) }.overrideMarker
      assertThat(initialOverrideMarker).isEqualTo(finalOverrideMarker)
    }
  }

  @Nested
  inner class NeedsAttention {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
    }

    @Test
    fun `should mark unmerged UUID as needs attention if it has additional records`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      val reactivatedPerson = createPerson(createRandomProbationPersonDetails(reactivatedCrn))
      val unmergedPerson = createPerson(createRandomProbationPersonDetails(unmergedCrn))
      val cluster = createPersonKey()
        .addPerson(unmergedPerson)
        .addPerson(createPerson(createRandomProbationPersonDetails()))

      val mergedReactivatedRecord = mergeRecord(reactivatedPerson, unmergedPerson)

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, mergedReactivatedRecord.crn!!, unmergedCrn)

      checkEventLog(unmergedPerson.crn!!, CPRLogEvents.CPR_RECLUSTER_NEEDS_ATTENTION) { eventLogs ->
        assertThat(eventLogs).hasSize(2)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.NEEDS_ATTENTION)
      }

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("CRN" to unmergedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_CREATED,
        mapOf("CRN" to reactivatedCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_UNMERGED,
        mapOf(
          "TO_SOURCE_SYSTEM_ID" to reactivatedCrn,
          "FROM_SOURCE_SYSTEM_ID" to unmergedCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )

      cluster.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION)
      cluster.assertClusterIsOfSize(2)

      unmergedPerson.assertLinkedToCluster(cluster)
      unmergedPerson.assertExcludedFrom(reactivatedPerson)

      reactivatedPerson.assertNotMerged()
      reactivatedPerson.assertHasLinkToCluster()
      reactivatedPerson.assertNotLinkedToCluster(cluster)
      reactivatedPerson.assertExcludedFrom(unmergedPerson)

      unmergedPerson.assertHasOverrideMarker()
      reactivatedPerson.assertHasOverrideMarker()
      unmergedPerson.assertHasDifferentOverrideMarker(reactivatedPerson)
      unmergedPerson.assertHasSameOverrideScope(reactivatedPerson)
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
        OFFENDER_UNMERGED,
        DomainEvent(
          eventType = OFFENDER_UNMERGED,
          additionalInformation = AdditionalInformation(
            reactivatedCrn = reactivatedCrn,
            unmergedCrn = unmergedCrn,
          ),
        ),
      )

      expectOneMessageOnDlq(probationMergeEventsQueue)
    }

    @Test
    fun `should push 404 to dead letter queue`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()
      stub404Response(probationUrl(unmergedCrn))

      publishDomainEvent(
        OFFENDER_UNMERGED,
        DomainEvent(
          eventType = OFFENDER_UNMERGED,
          additionalInformation = AdditionalInformation(
            reactivatedCrn = reactivatedCrn,
            unmergedCrn = unmergedCrn,
          ),
        ),
      )
      expectOneMessageOnDlq(probationMergeEventsQueue)
    }
  }
}
