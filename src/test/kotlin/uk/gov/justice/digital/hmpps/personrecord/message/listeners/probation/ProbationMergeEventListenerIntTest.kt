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
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_MERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ProbationMergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Nested
  inner class MissingFromRecord {

    @Test
    fun `processes offender merge event with source record does not exist`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()
      val targetPerson = createPersonWithNewKey(createRandomProbationPersonDetails(targetCrn))

      stubPersonMatchUpsert()
      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_SOURCE_SYSTEM_ID" to targetCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkEventLogExist(targetPerson.crn!!, CPRLogEvents.CPR_RECORD_UPDATED)
    }
  }

  @Nested
  inner class MissingToRecord {

    @Test
    fun `processes offender merge event with target record does not exist`() {
      val targetCrn = randomCrn()
      val sourcePerson = createPersonWithNewKey(createRandomProbationPersonDetails())

      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourcePerson.crn!!, targetCrn)

      val targetPerson = awaitNotNullPerson { personRepository.findByCrn(targetCrn) }
      sourcePerson.assertMergedTo(targetPerson)

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf(
          "CRN" to targetCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePerson.crn,
          "TO_SOURCE_SYSTEM_ID" to targetPerson.crn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkEventLogExist(targetPerson.crn!!, CPRLogEvents.CPR_RECORD_CREATED)
      checkEventLogExist(sourcePerson.crn!!, CPRLogEvents.CPR_RECORD_MERGED)
    }
  }

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubDeletePersonMatch()
    }

    @Test
    fun `processes offender merge event with records with same UUID is published`() {
      val sourcePerson = createPerson(createRandomProbationPersonDetails())
      val targetPerson = createPerson(createRandomProbationPersonDetails())
      val cluster = createPersonKey()
        .addPerson(sourcePerson)
        .addPerson(targetPerson)

      probationMergeEventAndResponseSetup(
        OFFENDER_MERGED,
        sourceCrn = sourcePerson.crn!!,
        targetCrn = targetPerson.crn!!,
      )

      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.assertNotLinkedToCluster()

      cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePerson.crn,
          "TO_SOURCE_SYSTEM_ID" to targetPerson.crn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkEventLogExist(sourcePerson.crn!!, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes offender merge event with different UUIDs where source has multiple records`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()

      val sourcePerson = createPerson(createRandomProbationPersonDetails(sourceCrn))
      val sourceCluster = createPersonKey()
        .addPerson(createPerson(createRandomProbationPersonDetails()))
        .addPerson(sourcePerson)

      val targetPerson = createPersonWithNewKey(createRandomProbationPersonDetails(targetCrn))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn)

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf(
          "CRN" to targetCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourceCrn,
          "TO_SOURCE_SYSTEM_ID" to targetCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkEventLogExist(targetCrn, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourceCrn, CPRLogEvents.CPR_RECORD_MERGED)

      sourceCluster.assertClusterStatus(UUIDStatusType.ACTIVE)
      sourceCluster.assertClusterIsOfSize(1)

      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.assertNotLinkedToCluster()

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)
    }

    @Test
    fun `processes offender merge event with different UUIDs where source doesn't have an UUID`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()

      val sourcePerson = createPerson(createRandomProbationPersonDetails(sourceCrn))
      val targetPerson = createPersonWithNewKey(createRandomProbationPersonDetails(targetCrn))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourceCrn,
          "TO_SOURCE_SYSTEM_ID" to targetCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkEventLogExist(targetCrn, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourceCrn, CPRLogEvents.CPR_RECORD_MERGED)

      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.assertNotLinkedToCluster()

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)
    }

    @Test
    fun `processes offender merge event with different UUIDs where source has a single record`() {
      val sourcePerson = createPersonWithNewKey(createRandomProbationPersonDetails())
      val targetPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourcePerson.crn!!, targetPerson.crn!!)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePerson.crn,
          "TO_SOURCE_SYSTEM_ID" to targetPerson.crn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkTelemetry(
        CPR_UUID_MERGED,
        mapOf(
          "FROM_UUID" to sourcePerson.personKey?.personUUID.toString(),
          "TO_UUID" to targetPerson.personKey?.personUUID.toString(),
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkEventLogExist(targetPerson.crn!!, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePerson.crn!!, CPRLogEvents.CPR_RECORD_MERGED)
      checkEventLog(sourcePerson.crn!!, CPRLogEvents.CPR_UUID_MERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val event = eventLogs.first()
        assertThat(event.personUUID).isEqualTo(sourcePerson.personKey?.personUUID)
        assertThat(event.uuidStatusType).isEqualTo(UUIDStatusType.MERGED)
      }

      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.personKey?.assertMergedTo(targetPerson.personKey!!)
      sourcePerson.personKey?.assertClusterStatus(UUIDStatusType.MERGED)
      sourcePerson.personKey?.assertClusterIsOfSize(0)

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)
    }
  }

  @Nested
  inner class RecoverableErrorHandling {

    @Test
    fun `should retry on 500 error from core person record and delius`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()
      stub5xxResponse(probationUrl(targetCrn), "next request will succeed", "retry")
      stubPersonMatchUpsert()
      stubDeletePersonMatch()
      val sourcePerson = createPerson(createRandomProbationPersonDetails(sourceCrn))
      val targetPerson = createPerson(createRandomProbationPersonDetails(targetCrn))
      createPersonKey()
        .addPerson(sourcePerson)
        .addPerson(targetPerson)

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn, scenario = "retry", currentScenarioState = "next request will succeed")

      expectNoMessagesOnQueueOrDlq(probationMergeEventsQueue)
      sourcePerson.assertMergedTo(targetPerson)
    }

    @Test
    fun `should retry on a 500 error from person match delete`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()
      val sourcePerson = createPerson(createRandomProbationPersonDetails(sourceCrn))
      val targetPerson = createPerson(createRandomProbationPersonDetails(targetCrn))
      createPersonKey()
        .addPerson(sourcePerson)
        .addPerson(targetPerson)

      // stubs for failed delete
      stubSingleProbationResponse(ApiResponseSetup.from(targetPerson), BASE_SCENARIO, "Started", "Started")
      stubDeletePersonMatch(status = 500, nextScenarioState = "deleteWillWork") // scenario state changes so next calls will succeed

      // stubs for successful delete
      stubPersonMatchUpsert(currentScenarioState = "deleteWillWork")
      stubDeletePersonMatch(currentScenarioState = "deleteWillWork")
      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn, currentScenarioState = "deleteWillWork", nextScenarioState = "deleteWillWork")

      sourcePerson.assertMergedTo(targetPerson)
    }

    @Test
    fun `should not throw error if person match returns a 404 on delete`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()
      val sourcePerson = createPerson(createRandomProbationPersonDetails(sourceCrn))
      val targetPerson = createPerson(createRandomProbationPersonDetails(targetCrn))
      createPersonKey()
        .addPerson(sourcePerson)
        .addPerson(targetPerson)

      stubDeletePersonMatch(status = 404)
      stubPersonMatchUpsert()
      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn)
      sourcePerson.assertMergedTo(targetPerson)
    }
  }

  @Nested
  inner class UnrecoverableErrorHandling {

    @Test
    fun `should put message on dlq when message processing fails`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()
      stub5xxResponse(probationUrl(targetCrn), nextScenarioState = "next request will fail", "failure")
      stub5xxResponse(
        probationUrl(targetCrn),
        nextScenarioState = "next request will fail",
        currentScenarioState = "next request will fail",
        scenarioName = "failure",
      )
      stub5xxResponse(
        probationUrl(targetCrn),
        nextScenarioState = "next request will fail",
        currentScenarioState = "next request will fail",
        scenarioName = "failure",
      )

      publishDomainEvent(
        OFFENDER_MERGED,
        DomainEvent(
          eventType = OFFENDER_MERGED,
          additionalInformation = AdditionalInformation(
            sourceCrn = sourceCrn,
            targetCrn = targetCrn,
          ),
        ),
      )

      expectOneMessageOnDlq(probationMergeEventsQueue)
    }

    @Test
    fun `check circular merge with 2 different UUIDS`() {
      val recordACrn = randomCrn()
      val recordBCrn = randomCrn()

      val recordA = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))
      val recordB = createPersonWithNewKey(createRandomProbationPersonDetails(recordBCrn))

      stubDeletePersonMatch()
      stubPersonMatchUpsert()
      probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordACrn, recordBCrn)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to recordACrn,
          "TO_SOURCE_SYSTEM_ID" to recordBCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordBCrn, recordACrn)

      expectOneMessageOnDlq(probationMergeEventsQueue)

      awaitAssert {
        val findRecordB = personKeyRepository.findByPersonUUID(recordB.personKey?.personUUID)
        assertThat(findRecordB?.mergedTo).isNull()
      }
    }
  }
}
