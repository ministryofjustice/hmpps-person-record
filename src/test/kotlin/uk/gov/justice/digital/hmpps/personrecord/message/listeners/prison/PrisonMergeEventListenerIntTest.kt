package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonMergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  private fun prisonURL(prisonNumber: String) = "/prisoner/$prisonNumber"

  @Nested
  inner class MissingFromRecord {

    @Test
    fun `processes prisoner merge event when source record does not exist`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      stubPersonMatchUpsert()
      prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber = sourcePrisonNumber, targetPrisonNumber = targetPrisonNumber)

      checkTelemetry(CPR_RECORD_UPDATED, mapOf("PRISON_NUMBER" to targetPrisonNumber))
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }
  }

  @Nested
  inner class MissingToRecord {

    @Test
    fun `processes prisoner merge event when target record does not exist`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))

      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
      prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber, targetPrisonNumber)

      val targetPerson = awaitNotNullPerson { personRepository.findByPrisonNumber(targetPrisonNumber) }

      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.assertNotLinkedToCluster()

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("PRISON_NUMBER" to targetPrisonNumber, "SOURCE_SYSTEM" to NOMIS.name),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_CREATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
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
    fun `processes prisoner merge event with records with same UUID is published`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPerson(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))
      createPersonKey()
        .addPerson(sourcePerson)
        .addPerson(targetPerson)

      prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber = sourcePrisonNumber, targetPrisonNumber = targetPrisonNumber)

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)
      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has multiple records`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val sourceCluster = createPersonKey()
        .addPerson(createPerson(Person(prisonNumber = randomPrisonNumber(), sourceSystem = NOMIS)))
        .addPerson(sourcePerson)
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber, targetPrisonNumber)

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      sourceCluster.assertClusterIsOfSize(1)
      sourceCluster.assertClusterStatus(UUIDStatusType.ACTIVE)

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("PRISON_NUMBER" to targetPrisonNumber, "SOURCE_SYSTEM" to NOMIS.name),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source doesn't have an UUID`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber, targetPrisonNumber)

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("PRISON_NUMBER" to targetPrisonNumber, "SOURCE_SYSTEM" to NOMIS.name),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has a single record`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber, targetPrisonNumber)

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.personKey?.assertClusterStatus(UUIDStatusType.MERGED)
      sourcePerson.personKey?.assertClusterIsOfSize(0)

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLog(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        assertThat(eventLogs.first().recordMergedTo).isEqualTo(targetPerson.id)
        assertThat(eventLogs.first().personUUID).isEqualTo(sourcePerson.personKey?.personUUID)
      }
      checkEventLog(sourcePrisonNumber, CPRLogEvents.CPR_UUID_MERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val event = eventLogs.first()
        assertThat(event.personUUID).isEqualTo(sourcePerson.personKey?.personUUID)
        assertThat(event.uuidStatusType).isEqualTo(UUIDStatusType.MERGED)
      }
    }

    @Test
    fun `should retry on 500 error`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      stub5xxResponse(prisonURL(targetPrisonNumber), "next request will succeed", "retry")

      prisonMergeEventAndResponseSetup(
        PRISONER_MERGED,
        sourcePrisonNumber,
        targetPrisonNumber,
        scenario = "retry",
        currentScenarioState = "next request will succeed",
      )

      expectNoMessagesOnQueueOrDlq(prisonMergeEventsQueue)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
    }
  }

  @Nested
  inner class ErrorHandling {

    @Test
    fun `should put on dlq when message processing fails`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      stub5xxResponse(prisonURL(targetPrisonNumber), "PrisonMergeEventProcessingWillFail", "failure")
      stub5xxResponse(prisonURL(targetPrisonNumber), "PrisonMergeEventProcessingWillFail", "failure", "PrisonMergeEventProcessingWillFail")
      stub5xxResponse(prisonURL(targetPrisonNumber), "PrisonMergeEventProcessingWillFail", "failure", "PrisonMergeEventProcessingWillFail")

      val additionalInformation =
        AdditionalInformation(sourcePrisonNumber = sourcePrisonNumber)
      val domainEvent = prisonDomainEvent(PRISONER_MERGED, targetPrisonNumber, additionalInformation)
      publishDomainEvent(PRISONER_MERGED, domainEvent)

      expectOneMessageOnDlq(prisonMergeEventsQueue)
    }
  }
}
