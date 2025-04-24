package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MERGE_RECORD_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class ProbationMergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Nested
  inner class SuccessfulProcessing {
    @BeforeEach
    fun beforeEach() {
      stubDeletePersonMatch()
    }

    @Test
    fun `processes offender merge event with records with same UUID is published`() {
      val sourcePerson = createPerson(createRandomProbationPersonDetails())
      val targetPerson = createPerson(createRandomProbationPersonDetails())
      val personKeyEntity = createPersonKey()
        .addPerson(sourcePerson)
        .addPerson(targetPerson)

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn = sourcePerson.crn!!, targetCrn = targetPerson.crn!!)

      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf("SOURCE_CRN" to sourcePerson.crn!!, "TARGET_CRN" to targetPerson.crn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_UUID" to personKeyEntity.personUUID.toString(),
          "FROM_UUID" to personKeyEntity.personUUID.toString(),
          "SOURCE_CRN" to sourcePerson.crn,
          "TARGET_CRN" to targetPerson.crn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )

      val mergedSourcePerson = personRepository.findByCrn(sourcePerson.crn!!)
      assertThat(mergedSourcePerson?.mergedTo).isEqualTo(targetPerson.id)

      checkEventLogExist(sourcePerson.crn!!, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes offender merge event with target record does not exist`() {
      val targetCrn = randomCrn()
      val sourcePerson = createPersonWithNewKey(createRandomProbationPersonDetails())

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourcePerson.crn!!, targetCrn)

      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf("SOURCE_CRN" to sourcePerson.crn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_MERGE_RECORD_NOT_FOUND,
        mapOf(
          "RECORD_TYPE" to "TARGET",
          "SOURCE_CRN" to sourcePerson.crn,
          "TARGET_CRN" to targetCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
    }

    @Test
    fun `processes offender merge event with different UUIDs where source has multiple records`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()

      val sourceCluster = createPersonKey()
        .addPerson(createPerson(createRandomProbationPersonDetails()))
        .addPerson(createPerson(createRandomProbationPersonDetails(sourceCrn)))

      val targetPerson = createPersonWithNewKey(createRandomProbationPersonDetails(targetCrn))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn)

      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_UUID" to targetPerson.personKey?.personUUID.toString(),
          "FROM_UUID" to sourceCluster.personUUID.toString(),
          "SOURCE_CRN" to sourceCrn,
          "TARGET_CRN" to targetCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkEventLogExist(sourceCrn, CPRLogEvents.CPR_RECORD_MERGED)

      val sourcePerson = personRepository.findByCrn(sourceCrn)
      assertThat(sourcePerson?.mergedTo).isEqualTo(targetPerson.id)
      assertThat(sourcePerson?.personKey).isNull()

      val updatedSourceCluster = personKeyRepository.findByPersonUUID(sourceCluster.personUUID)
      assertThat(updatedSourceCluster?.personEntities?.size).isEqualTo(1)

      val targetCluster = personKeyRepository.findByPersonUUID(targetPerson.personKey?.personUUID)
      assertThat(targetCluster?.personEntities?.size).isEqualTo(1)
    }

    @Test
    fun `processes offender merge event with different UUIDs where source doesn't have an UUID`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()

      createPerson(createRandomProbationPersonDetails(sourceCrn))
      val targetPerson = createPersonWithNewKey(createRandomProbationPersonDetails(targetCrn))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn)

      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_UUID" to targetPerson.personKey?.personUUID.toString(),
          "FROM_UUID" to null,
          "SOURCE_CRN" to sourceCrn,
          "TARGET_CRN" to targetCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )

      val sourcePerson = personRepository.findByCrn(sourceCrn)
      assertThat(sourcePerson?.mergedTo).isEqualTo(targetPerson.id)
      assertThat(sourcePerson?.personKey).isNull()

      val targetCluster = personKeyRepository.findByPersonUUID(targetPerson.personKey?.personUUID)
      assertThat(targetCluster?.personEntities?.size).isEqualTo(1)
    }

    @Test
    fun `processes offender merge event with different UUIDs where source has a single record`() {
      val sourcePerson = createPersonWithNewKey(createRandomProbationPersonDetails())
      val targetPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourcePerson.crn!!, targetPerson.crn!!)

      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf("SOURCE_CRN" to sourcePerson.crn, "TARGET_CRN" to targetPerson.crn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_UUID" to targetPerson.personKey?.personUUID.toString(),
          "FROM_UUID" to sourcePerson.personKey?.personUUID.toString(),
          "SOURCE_CRN" to sourcePerson.crn,
          "TARGET_CRN" to targetPerson.crn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
      checkEventLogExist(sourcePerson.crn!!, CPRLogEvents.CPR_RECORD_MERGED)

      val mergedSourcePerson = personRepository.findByCrn(sourcePerson.crn!!)
      assertThat(mergedSourcePerson?.mergedTo).isEqualTo(targetPerson.id)
      assertThat(mergedSourcePerson?.personKey?.mergedTo).isEqualTo(targetPerson.personKey?.id)
      assertThat(mergedSourcePerson?.personKey?.status).isEqualTo(UUIDStatusType.MERGED)
    }

    @Test
    fun `should retry on 500 error`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()
      stub5xxResponse(probationUrl(targetCrn), "next request will succeed", "retry")

      val personKeyEntity = createPersonKey()
        .addPerson(createPerson(createRandomProbationPersonDetails(sourceCrn)))
        .addPerson(createPerson(createRandomProbationPersonDetails(targetCrn)))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn, scenario = "retry", currentScenarioState = "next request will succeed")

      expectNoMessagesOnQueueOrDlq(probationMergeEventsQueue)

      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_UUID" to personKeyEntity.personUUID.toString(),
          "FROM_UUID" to personKeyEntity.personUUID.toString(),
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
    }

    @Test
    fun `should not throw error if person match returns a 404 on delete`() {
      val sourceCrn = randomCrn()
      val targetCrn = randomCrn()
      val personKeyEntity = createPersonKey()
        .addPerson(createPerson(createRandomProbationPersonDetails(sourceCrn)))
        .addPerson(createPerson(createRandomProbationPersonDetails(targetCrn)))

      stubDeletePersonMatch(status = 404)
      probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_UUID" to personKeyEntity.personUUID.toString(),
          "FROM_UUID" to personKeyEntity.personUUID.toString(),
          "SOURCE_CRN" to sourceCrn,
          "TARGET_CRN" to targetCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )
    }
  }

  @Test
  fun `should log when message processing fails`() {
    val sourceCrn = randomCrn()
    val targetCrn = randomCrn()
    stub5xxResponse(probationUrl(targetCrn), nextScenarioState = "next request will fail", "failure")
    stub5xxResponse(probationUrl(targetCrn), nextScenarioState = "next request will fail", currentScenarioState = "next request will fail", scenarioName = "failure")
    stub5xxResponse(probationUrl(targetCrn), nextScenarioState = "next request will fail", currentScenarioState = "next request will fail", scenarioName = "failure")

    val messageId = publishDomainEvent(
      OFFENDER_MERGED,
      DomainEvent(
        eventType = OFFENDER_MERGED,
        additionalInformation = AdditionalInformation(
          sourceCrn = sourceCrn,
          targetCrn = targetCrn,
        ),
      ),
    )

    purgeQueueAndDlq(probationMergeEventsQueue)

    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      MESSAGE_PROCESSING_FAILED,
      mapOf(
        "SOURCE_SYSTEM" to "DELIUS",
        EventKeys.EVENT_TYPE.toString() to OFFENDER_MERGED,
        EventKeys.MESSAGE_ID.toString() to messageId,
      ),
    )
  }

  @Test
  fun `processes offender merge event with source record does not exist`() {
    val sourceCrn = randomCrn()
    val targetCrn = randomCrn()
    val targetPerson = createPersonWithNewKey(createRandomProbationPersonDetails(targetCrn))

    probationMergeEventAndResponseSetup(OFFENDER_MERGED, sourceCrn, targetCrn)

    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_MERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_MERGE_RECORD_NOT_FOUND,
      mapOf(
        "RECORD_TYPE" to "SOURCE",
        "SOURCE_CRN" to sourceCrn,
        "TARGET_CRN" to targetCrn,
        "SOURCE_SYSTEM" to "DELIUS",
      ),
    )
    checkTelemetry(
      CPR_RECORD_MERGED,
      mapOf(
        "TO_UUID" to targetPerson.personKey?.personUUID.toString(),
        "FROM_UUID" to null,
        "SOURCE_CRN" to sourceCrn,
        "TARGET_CRN" to targetCrn,
        "SOURCE_SYSTEM" to "DELIUS",
      ),
    )
  }

  @Test
  fun `check circular merge with 2 different UUIDS`() {
    stubDeletePersonMatch()
    val recordACrn = randomCrn()
    val recordBCrn = randomCrn()

    val recordA = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))
    val recordB = createPersonWithNewKey(createRandomProbationPersonDetails(recordBCrn))

    probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordACrn, recordBCrn)

    checkTelemetry(
      CPR_RECORD_MERGED,
      mapOf(
        "TO_UUID" to recordB.personKey?.personUUID.toString(),
        "FROM_UUID" to recordA.personKey?.personUUID.toString(),
        "SOURCE_CRN" to recordACrn,
        "TARGET_CRN" to recordBCrn,
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
