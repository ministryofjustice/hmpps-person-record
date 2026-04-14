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
    @Test
    fun `deleting a merged from person does not delete the merged to person`() {
      val recordACrn = randomCrn()
      val recordBCrn = randomCrn()

      // Record Cluster (2 Records - B merged to A)
      val recordA = createPerson(createRandomProbationPersonDetails(recordACrn))
      val cluster = createPersonKey()
        .addPerson(recordA)
      val recordB = createPerson(createRandomProbationPersonDetails(recordBCrn))

      mergeRecord(recordB, recordA)

      publishProbationDomainEvent(OFFENDER_DELETION, recordBCrn)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordBCrn, "UUID" to null, "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_DELETED)

      recordB.assertPersonDeleted()
      cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster.assertClusterIsOfSize(1)
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

      mergeRecord(recordB, recordA)

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

    @Test // TODO: ???
    fun `should process offender delete with 2 records which have merged on different UUIDs`() {
      val recordACrn = randomCrn()
      val recordBCrn = randomCrn()

      val mergedTo = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))
      val mergedFrom = createPersonWithNewKey(createRandomProbationPersonDetails(recordBCrn))
      val mergedFromPersonKey = mergedFrom.personKey
      mergeRecord(mergedFrom, mergedTo)

      publishProbationDomainEvent(OFFENDER_DELETION, recordACrn)

      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordACrn, "UUID" to mergedTo.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_DELETED,
        mapOf("CRN" to recordACrn, "UUID" to mergedTo.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_RECORD_DELETED,
        mapOf("CRN" to recordBCrn, "UUID" to mergedFrom.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )
      checkTelemetry(
        CPR_UUID_DELETED,
        mapOf("CRN" to recordBCrn, "UUID" to mergedFrom.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
      )

      checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_DELETED)
      checkEventLogExist(recordACrn, CPRLogEvents.CPR_UUID_DELETED)
      checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_DELETED)

      mergedFrom.assertPersonDeleted()
      mergedTo.assertPersonDeleted()
      mergedFromPersonKey!!.assertPersonKeyDeleted()
      // this is a bug I think. mergedFrom's personKey has been merged to mergedTo's personKey but nothing in the DeletionService checks this to delete it
      // this used to pass because the test setup was incorrect and left the personKey on the merged record set. In reality it would be null
      mergedTo.personKey?.assertPersonKeyDeleted()
    }

    @Test
    fun `when record c is merged to record b and record b is merged to a - deleting record a - deletes all`() {
      // Merged Record Chain: C -> B -> A
      val recordACrn = randomCrn()
      val recordBCrn = randomCrn()
      val recordCCrn = randomCrn()

      // First Record Cluster (1 Record)
      val recordA = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))

      // Second Record Cluster (2 Records - C merged to B)
      val recordB = createPersonWithNewKey(createRandomProbationPersonDetails(recordBCrn))
      val recordBPersonKey = recordB.personKey
      val recordC = createPerson(createRandomProbationPersonDetails(recordCCrn))

      mergeRecord(recordC, recordB)
      mergeRecord(recordB, recordA)

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
      recordBPersonKey!!.assertPersonKeyDeleted()
      // this is a bug I think. recordB's personKey has been merged to recordA's personKey but nothing in the DeletionService checks this to delete it
      // this used to pass because the test setup was incorrect and left the personKey on the merged record set. In reality it would be null
    }

    @Test
    fun `when record b is merged to a and record c is merged to a - deleting record a - deletes all`() {
      val recordACrn = randomCrn()
      val recordBCrn = randomCrn()
      val recordCCrn = randomCrn()

      // Record Cluster (3 Records - B -> A <- C)
      val recordA = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))
      val recordB = createPerson(createRandomProbationPersonDetails(recordBCrn))
      val recordC = createPerson(createRandomProbationPersonDetails(recordCCrn))

      mergeRecord(recordB, recordA)
      mergeRecord(recordC, recordA)

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
      val recordB = createPerson(createRandomProbationPersonDetails(recordBCrn))
      val recordC = createPerson(createRandomProbationPersonDetails(recordCCrn))
      val recordD = createPerson(createRandomProbationPersonDetails(recordDCrn))

      mergeRecord(recordD, recordC)
      mergeRecord(recordC, recordA)
      mergeRecord(recordB, recordA)

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
