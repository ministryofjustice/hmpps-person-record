package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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

  @Test
  fun `should process offender GDPR delete`() {
    val crn = randomCrn()
    val domainEvent = probationDomainEvent(OFFENDER_GDPR_DELETION, crn)

    val person = createPerson(createRandomProbationPersonDetails(crn))
    val personKey = createPersonKey()
      .addPerson(person)
      .addPerson(createRandomProbationPersonDetails())

    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

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
  fun `should process offender delete with 1 record on single UUID`() {
    val crn = randomCrn()
    val domainEvent = probationDomainEvent(OFFENDER_DELETION, crn)
    val person = createPersonWithNewKey(createRandomProbationPersonDetails(crn))

    publishDomainEvent(OFFENDER_DELETION, domainEvent)

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
  fun `should process offender delete with multiple records on single UUID`() {
    val crn = randomCrn()
    val domainEvent = probationDomainEvent(OFFENDER_DELETION, crn)

    val person = createPerson(createRandomProbationPersonDetails(crn))
    val personKey = createPersonKey()
      .addPerson(person)
      .addPerson(createRandomProbationPersonDetails())

    publishDomainEvent(OFFENDER_DELETION, domainEvent)

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
  fun `should process offender delete with a merged record on single UUID`() {
    val recordACrn = randomCrn()
    val recordBCrn = randomCrn()

    // Delete Record B
    val domainEvent = probationDomainEvent(OFFENDER_DELETION, recordBCrn)

    // Record Cluster (2 Records - B merged to A)
    val recordA = createPerson(createRandomProbationPersonDetails(recordACrn))
    val cluster = createPersonKey()
      .addPerson(recordA)
    val recordB = createPerson(createRandomProbationPersonDetails(recordBCrn))

    mergeRecord(recordB, recordA)

    publishDomainEvent(OFFENDER_DELETION, domainEvent)

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
  fun `should process offender delete with 2 records which have merged on a single UUID`() {
    val recordACrn = randomCrn()
    val recordBCrn = randomCrn()
    val domainEvent = probationDomainEvent(OFFENDER_DELETION, recordACrn)

    // Record Cluster (1 Record - B merged to A)
    val recordA = createPerson(createRandomProbationPersonDetails(recordACrn))
    val recordB = createPerson(createRandomProbationPersonDetails(recordBCrn))
    val cluster = createPersonKey()
      .addPerson(recordA)
      .addPerson(recordB)

    mergeRecord(recordB, recordA)

    publishDomainEvent(OFFENDER_DELETION, domainEvent)

    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordACrn, "UUID" to cluster.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to cluster.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to cluster.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_DELETED)
    checkEventLogExist(recordBCrn, CPRLogEvents.CPR_UUID_DELETED)

    recordA.assertPersonDeleted()
    recordB.assertPersonDeleted()
    cluster.assertPersonKeyDeleted()
  }

  @Test
  fun `should process offender delete with 2 records which have merged on different UUIDs`() {
    val recordACrn = randomCrn()
    val recordBCrn = randomCrn()
    val domainEvent = probationDomainEvent(OFFENDER_DELETION, recordACrn)

    val mergedTo = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))
    val mergedFrom = createPersonWithNewKey(createRandomProbationPersonDetails(recordBCrn))

    mergeRecord(mergedFrom, mergedTo)
    mergeUuid(mergedFrom.personKey!!, mergedTo.personKey!!)

    publishDomainEvent(OFFENDER_DELETION, domainEvent)

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
    checkEventLogExist(recordBCrn, CPRLogEvents.CPR_UUID_DELETED)

    mergedFrom.assertPersonDeleted()
    mergedTo.assertPersonDeleted()
    mergedFrom.personKey?.assertPersonKeyDeleted()
    mergedTo.personKey?.assertPersonKeyDeleted()
  }

  @Test
  fun `should process offender delete with 3 records which have merged on different UUIDs`() {
    // Merged Record Chain: C -> B -> A
    val recordACrn = randomCrn()
    val recordBCrn = randomCrn()
    val recordCCrn = randomCrn()
    val domainEvent = probationDomainEvent(OFFENDER_DELETION, recordACrn)

    // First Record Cluster (1 Record)
    val recordA = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))

    // Second Record Cluster (2 Records - C merged to B)
    val recordB = createPersonWithNewKey(createRandomProbationPersonDetails(recordBCrn))
    val recordC = createPerson(createRandomProbationPersonDetails(recordCCrn))

    mergeRecord(recordC, recordB)
    mergeRecord(recordB, recordA)
    mergeUuid(recordB.personKey!!, recordA.personKey!!)

    publishDomainEvent(OFFENDER_DELETION, domainEvent)

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
      mapOf("CRN" to recordBCrn, "UUID" to recordB.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to recordB.personKey?.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_DELETED)
    checkEventLogExist(recordACrn, CPRLogEvents.CPR_UUID_DELETED)
    checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_DELETED)
    checkEventLogExist(recordBCrn, CPRLogEvents.CPR_UUID_DELETED)
    checkEventLogExist(recordCCrn, CPRLogEvents.CPR_RECORD_DELETED)

    recordA.assertPersonDeleted()
    recordB.assertPersonDeleted()
    recordC.assertPersonDeleted()
    recordA.personKey?.assertPersonKeyDeleted()
    recordB.personKey?.assertPersonKeyDeleted()
  }

  @Test
  fun `should process offender delete when multiple records merged on single record`() {
    val recordACrn = randomCrn()
    val recordBCrn = randomCrn()
    val recordCCrn = randomCrn()

    val domainEvent = probationDomainEvent(OFFENDER_DELETION, recordACrn)

    // Record Cluster (3 Records - B -> A <- C)
    val recordA = createPersonWithNewKey(createRandomProbationPersonDetails(recordACrn))
    val recordB = createPerson(createRandomProbationPersonDetails(recordBCrn))
    val recordC = createPerson(createRandomProbationPersonDetails(recordCCrn))

    mergeRecord(recordB, recordA)
    mergeRecord(recordC, recordA)

    publishDomainEvent(OFFENDER_DELETION, domainEvent)

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
  fun `should process offender delete when 2 records merged on cluster with other active records`() {
    val recordACrn = randomCrn()
    val recordBCrn = randomCrn()
    val recordCCrn = randomCrn()

    val domainEvent = probationDomainEvent(OFFENDER_DELETION, recordACrn)

    // Record Cluster (3 Records - B -> A)
    val recordA = createPerson(createRandomProbationPersonDetails(recordACrn))
    val recordB = createPerson(createRandomProbationPersonDetails(recordBCrn))
    val cluster = createPersonKey()
      .addPerson(recordA)
      .addPerson(recordB)
      .addPerson(createRandomProbationPersonDetails(recordCCrn))

    mergeRecord(recordB, recordA)

    publishDomainEvent(OFFENDER_DELETION, domainEvent)

    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordACrn, "UUID" to cluster.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to cluster.personUUID.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkEventLogExist(recordACrn, CPRLogEvents.CPR_RECORD_DELETED)
    checkEventLogExist(recordBCrn, CPRLogEvents.CPR_RECORD_DELETED)

    recordA.assertPersonDeleted()
    recordB.assertPersonDeleted()
    cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
    cluster.assertClusterIsOfSize(1)
  }

  @Test
  fun `should process offender delete with override marker`() {
    val personA = createPersonWithNewKey(createRandomProbationPersonDetails())
    val personB = createPersonWithNewKey(createRandomProbationPersonDetails())

    stubPersonMatchUpsert()
    excludeRecord(personA, personB)

    val domainEvent = probationDomainEvent(OFFENDER_DELETION, personA.crn!!)
    publishDomainEvent(OFFENDER_DELETION, domainEvent)

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
