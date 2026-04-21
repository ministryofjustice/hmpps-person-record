package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_GDPR_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_DELETED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class ProbationDeleteListenerIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun beforeEach() {
    stubDeletePersonMatch()
  }

  @Nested
  inner class NonMergeScenarios {
    @Test
    fun `deletes person with a GDPR event`() {
      val crn = randomCrn()
      val person = createPerson(createRandomProbationPersonDetails(crn))
      val personKey = createPersonKey()
        .addPerson(person)
        .addPerson(createRandomProbationPersonDetails())

      publishProbationDomainEvent(OFFENDER_GDPR_DELETION, crn)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to crn, "UUID" to personKey.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_DELETED)

      person.assertPersonDeleted()
      personKey.assertClusterStatus(UUIDStatusType.ACTIVE)
      personKey.assertClusterIsOfSize(1)
    }

    @Test
    fun `when cluster has one person - deletes person and cluster`() {
      val crn = randomCrn()
      val person = createPersonWithNewKey(createRandomProbationPersonDetails(crn))
      publishProbationDomainEvent(OFFENDER_DELETION, crn)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to crn, "UUID" to person.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_DELETED,
        mapOf("CRN" to crn, "UUID" to person.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkEventLog(crn, CPRLogEvents.CPR_UUID_DELETED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(person.personKey?.personUUID)
      }
      checkEventLog(crn, CPRLogEvents.CPR_RECORD_DELETED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isEqualTo(person.personKey?.personUUID)
      }

      person.assertPersonDeleted()
      person.personKey?.assertPersonKeyDeleted()
    }

    @Test
    fun `when cluster has more than one person - delete person only`() {
      val crn = randomCrn()

      val person = createPerson(createRandomProbationPersonDetails(crn))
      val personKey = createPersonKey()
        .addPerson(person)
        .addPerson(createRandomProbationPersonDetails())

      publishProbationDomainEvent(OFFENDER_DELETION, crn)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to crn, "UUID" to personKey.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_DELETED)

      person.assertPersonDeleted()
      personKey.assertClusterStatus(UUIDStatusType.ACTIVE)
      personKey.assertClusterIsOfSize(1)
    }
  }

  @Nested
  inner class MergeScenarios {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
    }

    @Test
    fun `deleting a merged from person does not delete the merged to person`() {
      val recordACrn = randomCrn()
      val recordBCrn = randomCrn()

      // Record Cluster (2 Records - B merged to A)
      val recordA = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))
      val recordB = createPersonWithNewKey(createRandomProbationPersonDetails(recordBCrn))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordBCrn, recordACrn)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_MERGED)

      publishProbationDomainEvent(OFFENDER_DELETION, recordBCrn)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordBCrn, "UUID" to null, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_DELETED)

      recordB.assertPersonDeleted()
      recordA.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      recordA.personKey?.assertClusterIsOfSize(1)
    }

    @Test
    fun `deleting a merged to person does delete a merged from person`() {
      val recordACrn = randomCrn()
      val recordBCrn = randomCrn()

      // Record Cluster (1 Record - B merged to A)
      val recordA = createPerson(createRandomProbationPersonDetails(recordACrn))
      val recordB = createPerson(createRandomProbationPersonDetails(recordBCrn))
      val cluster = createPersonKey()
        .addPerson(recordA)
        .addPerson(recordB)

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordBCrn, recordACrn)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_MERGED)

      publishProbationDomainEvent(OFFENDER_DELETION, recordACrn)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordACrn, "UUID" to cluster.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordBCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_DELETED,
        mapOf("CRN" to recordACrn, "UUID" to cluster.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_UUID_DELETED)

      recordA.assertPersonDeleted()
      recordB.assertPersonDeleted()
      cluster.assertPersonKeyDeleted()
    }

    @Test
    fun `should process offender delete with 2 records which have merged on different UUIDs`() {
      val mergedToCrn = randomCrn()
      val mergedFromCrn = randomCrn()

      val mergedTo = createPersonWithNewKey(createRandomProbationPersonDetails(mergedToCrn))
      val mergedFrom = createPersonWithNewKey(createRandomProbationPersonDetails(mergedFromCrn))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, mergedFromCrn, mergedToCrn)
      checkEventLogExist(mergedToCrn, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(mergedFromCrn, CPRLogEvents.CPR_RECORD_MERGED)

      publishProbationDomainEvent(OFFENDER_DELETION, mergedToCrn)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to mergedToCrn, "UUID" to mergedTo.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_DELETED,
        mapOf("CRN" to mergedToCrn, "UUID" to mergedTo.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to mergedFromCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )

      checkEventLogExist(mergedToCrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(mergedToCrn, CPRLogEvents.CPR_UUID_DELETED)
      checkEventLogExist(mergedFromCrn, CPRLogEvents.CPR_RECORD_DELETED)

      mergedFrom.assertPersonDeleted()
      mergedTo.assertPersonDeleted()
      mergedFrom.personKey?.assertPersonKeyDeleted()
      mergedTo.personKey?.assertPersonKeyDeleted()
    }

    @Test
    fun `when record c is merged to record b and record b is merged to a - deleting record a - deletes all`() {
      // Merged Record Chain: C -> B -> A
      val recordACrn = randomCrn()
      val recordBCrn = randomCrn()
      val recordCCrn = randomCrn()

      val recordA = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))
      val recordB = createPersonWithNewKey(createRandomProbationPersonDetails(recordBCrn))
      val recordC = createPersonWithNewKey(createRandomProbationPersonDetails(recordCCrn))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordCCrn, recordBCrn)
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(recordCCrn, CPRLogEvents.CPR_RECORD_MERGED)

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordBCrn, recordACrn)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_MERGED)

      publishProbationDomainEvent(OFFENDER_DELETION, recordACrn)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordACrn, "UUID" to recordA.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_DELETED,
        mapOf("CRN" to recordACrn, "UUID" to recordA.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordBCrn, "SOURCE_SYSTEM" to "DELIUS"),
      )

      checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_UUID_DELETED)
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(recordCCrn, CPRLogEvents.CPR_RECORD_DELETED)

      recordA.assertPersonDeleted()
      recordB.assertPersonDeleted()
      recordC.assertPersonDeleted()
      recordA.personKey?.assertPersonKeyDeleted()
      recordB.personKey?.assertPersonKeyDeleted()
    }

    @Test
    fun `when record b is merged to a and record c is merged to a - deleting record a - deletes all`() {
      val recordACrn = randomCrn()
      val recordBCrn = randomCrn()
      val recordCCrn = randomCrn()

      // Record Cluster (3 Records - B -> A <- C)
      val recordA = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))
      val recordB = createPersonWithNewKey(createRandomProbationPersonDetails(recordBCrn))
      val recordC = createPersonWithNewKey(createRandomProbationPersonDetails(recordCCrn))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordBCrn, recordACrn)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_MERGED)

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordCCrn, recordACrn)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_UPDATED, 2)
      checkEventLogExist(recordCCrn, CPRLogEvents.CPR_RECORD_MERGED)

      publishProbationDomainEvent(OFFENDER_DELETION, recordACrn)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordACrn, "UUID" to recordA.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_DELETED,
        mapOf("CRN" to recordACrn, "UUID" to recordA.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordBCrn, "UUID" to null, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordCCrn, "UUID" to null, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(recordCCrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_UUID_DELETED)

      recordA.assertPersonDeleted()
      recordB.assertPersonDeleted()
      recordC.assertPersonDeleted()
      recordA.personKey?.assertPersonKeyDeleted()
    }

    @Test
    fun `when record d is merged to c and c is merged to a and b is merged to a - deleting record a - deletes all`() {
      val recordACrn = randomCrn()
      val recordBCrn = randomCrn()
      val recordCCrn = randomCrn()
      val recordDCrn = randomCrn()

      val recordA = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))
      val recordB = createPersonWithNewKey(createRandomProbationPersonDetails(recordBCrn))
      val recordC = createPersonWithNewKey(createRandomProbationPersonDetails(recordCCrn))
      val recordD = createPersonWithNewKey(createRandomProbationPersonDetails(recordDCrn))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordDCrn, recordCCrn)
      checkEventLogExist(recordCCrn, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(recordDCrn, CPRLogEvents.CPR_RECORD_MERGED)

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordCCrn, recordACrn)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(recordCCrn, CPRLogEvents.CPR_RECORD_MERGED)

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, recordBCrn, recordACrn)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_UPDATED, 2)
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_MERGED)

      publishProbationDomainEvent(OFFENDER_DELETION, recordACrn)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordACrn, "UUID" to recordA.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_DELETED,
        mapOf("CRN" to recordACrn, "UUID" to recordA.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordBCrn, "UUID" to null, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordCCrn, "UUID" to null, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordDCrn, "UUID" to null, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(recordCCrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(recordDCrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_UUID_DELETED)

      recordA.assertPersonDeleted()
      recordB.assertPersonDeleted()
      recordC.assertPersonDeleted()
      recordD.assertPersonDeleted()
      recordA.personKey?.assertPersonKeyDeleted()
    }
  }

  @Test
  fun `should process offender delete with override marker`() {
    val personA = createPersonWithNewKey(createRandomProbationPersonDetails())
    val personB = createPersonWithNewKey(createRandomProbationPersonDetails())

    stubPersonMatchUpsert()
    excludeRecord(personA, personB)

    publishProbationDomainEvent(OFFENDER_DELETION, personA.crn!!)

    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to personA.crn, "UUID" to personA.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to personA.crn, "UUID" to personA.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )

    personA.assertPersonDeleted()
    personA.personKey?.assertPersonKeyDeleted()

    personB.assertHasOverrideMarker()
    personB.assertOverrideScopeSize(1)
  }
}
