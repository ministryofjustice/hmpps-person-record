package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.MERGED
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
      val (targetPerson) = createPersonAndKey(createRandomProbationPersonDetails(targetCrn))

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
      val (sourcePerson) = createPersonAndKey(createRandomProbationPersonDetails())

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
    fun `processes offender merge event with different UUIDs where source has multiple records`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()

      val sourcePerson = createPerson(createRandomProbationPersonDetails(sourceCrn))
      val sourceCluster = createPersonKey()
        .addPerson(createPerson(createRandomProbationPersonDetails()))
        .addPerson(sourcePerson)

      val (targetPerson, targetPersonKey) = createPersonAndKey(createRandomProbationPersonDetails(targetCrn))

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

      sourceCluster.assertClusterStatus(ACTIVE)
      sourceCluster.assertClusterIsOfSize(1)

      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.assertNotLinkedToCluster()

      targetPersonKey.assertClusterStatus(ACTIVE)
      targetPersonKey.assertClusterIsOfSize(1)
    }

    @Test
    fun `processes offender merge event with different UUIDs where source doesn't have an UUID`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()

      val sourcePerson = createPerson(createRandomProbationPersonDetails(sourceCrn))
      val (targetPerson, targetPersonKey) = createPersonAndKey(createRandomProbationPersonDetails(targetCrn))

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

      targetPersonKey.assertClusterStatus(ACTIVE)
      targetPersonKey.assertClusterIsOfSize(1)
    }

    @Test
    fun `processes offender merge event with different UUIDs where source has a single record`() {
      val (sourcePerson, sourcePersonKey) = createPersonAndKey(createRandomProbationPersonDetails())
      val (targetPerson, targetPersonKey) = createPersonAndKey(createRandomProbationPersonDetails())

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
          "FROM_UUID" to sourcePersonKey.personUUID.toString(),
          "TO_UUID" to targetPersonKey.personUUID.toString(),
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkEventLogExist(targetPerson.crn!!, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePerson.crn!!, CPRLogEvents.CPR_RECORD_MERGED)
      checkEventLog(sourcePerson.crn!!, CPRLogEvents.CPR_UUID_MERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val event = eventLogs.first()
        assertThat(event.personUUID).isEqualTo(sourcePersonKey.personUUID)
        assertThat(event.uuidStatusType).isEqualTo(MERGED)
      }

      sourcePerson.assertMergedTo(targetPerson)
      sourcePersonKey.assertMergedTo(targetPerson.personKey!!)
      sourcePersonKey.assertClusterStatus(MERGED)
      sourcePersonKey.assertClusterIsOfSize(0)

      targetPersonKey.assertClusterStatus(ACTIVE)
      targetPersonKey.assertClusterIsOfSize(1)
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
      val sourcePerson = createRandomProbationPersonDetails(sourceCrn)
      val sourcePersonEntity = createPerson(sourcePerson)
      val targetPerson = createRandomProbationPersonDetails(targetCrn)
      val targetPersonEntity = createPerson(targetPerson)
      createPersonKey()
        .addPerson(sourcePersonEntity)
        .addPerson(targetPersonEntity)

      // stubs for failed delete
      val response = ApiResponseSetup.from(targetPerson)
      stubSingleProbationResponse(response, BASE_SCENARIO, "Started", "Started")
      stubDeletePersonMatch(status = 500, nextScenarioState = "deleteWillWork") // scenario state changes so next calls will succeed

      // stubs for successful delete
      stubDeletePersonMatch(currentScenarioState = "deleteWillWork")
      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn, currentScenarioState = "deleteWillWork", nextScenarioState = "deleteWillWork", apiResponseSetup = response)

      sourcePersonEntity.assertMergedTo(targetPersonEntity)
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

      createPersonAndKey(createRandomProbationPersonDetails(recordACrn))
      val (_, recordBKey) = createPersonAndKey(createRandomProbationPersonDetails(recordBCrn))

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
        val findRecordB = personKeyRepository.findByPersonUUID(recordBKey.personUUID)
        assertThat(findRecordB?.mergedTo).isNull()
      }
    }
  }
}
