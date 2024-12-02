package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_GDPR_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit.SECONDS

class ProbationDeleteListenerIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun beforeEach() {
    telemetryRepository.deleteAll()
  }

  @Test
  fun `should process offender delete with 1 record on single UUID`() {
    val crn = randomCRN()
    val domainEvent = buildDomainEvent(crn)
    val personKey = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
      personKeyEntity = personKey,
    )
    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("CRN" to crn, "EVENT_TYPE" to OFFENDER_GDPR_DELETION, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to crn, "UUID" to personKey.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to crn, "UUID" to personKey.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )

    await untilAsserted { assertThat(personRepository.findByCrn(crn)).isNull() }
    await untilAsserted { assertThat(personKeyRepository.findByPersonId(personKey.personId)).isNull() }
  }

  @Test
  fun `should process offender delete with multiple records on single UUID`() {
    val crn = randomCRN()
    val domainEvent = buildDomainEvent(crn)
    val personKey = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
      personKeyEntity = personKey,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKey,
    )
    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("CRN" to crn, "EVENT_TYPE" to OFFENDER_GDPR_DELETION, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to crn, "UUID" to personKey.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )

    await.atMost(4, SECONDS) untilAsserted { assertThat(personRepository.findByCrn(crn)).isNull() }

    val updatedCluster = personKeyRepository.findByPersonId(personKey.personId)
    assertThat(updatedCluster).isNotNull()
    assertThat(updatedCluster?.personEntities?.size).isEqualTo(1)
    assertThat(updatedCluster?.personEntities?.first()?.crn).isNotEqualTo(crn)
  }

  @Test
  fun `should process offender delete with a merged record on single UUID`() {
    val recordACrn = randomCRN()
    val recordBCrn = randomCRN()

    // Delete Record B
    val domainEvent = buildDomainEvent(recordBCrn)

    // Record Cluster (2 Records - B merged to A)
    val cluster = createPersonKey()
    val recordA = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordACrn))),
      personKeyEntity = cluster,
    )
    val recordB = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordBCrn))),
    )
    mergeRecord(recordB, recordA)
    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("CRN" to recordBCrn, "EVENT_TYPE" to OFFENDER_GDPR_DELETION, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to null, "SOURCE_SYSTEM" to "DELIUS"),
    )

    await untilAsserted { assertThat(personRepository.findByCrn(recordBCrn)).isNull() }
    await untilAsserted { assertThat(personRepository.findByCrn(recordACrn)).isNotNull() }
    await untilAsserted { assertThat(personKeyRepository.findByPersonId(cluster.personId)).isNotNull() }
  }

  @Test
  fun `should process offender delete with 2 records which have merged on a single UUID`() {
    val recordACrn = randomCRN()
    val recordBCrn = randomCRN()
    val domainEvent = buildDomainEvent(recordACrn)

    // Record Cluster (1 Record - B merged to A)
    val cluster = createPersonKey()
    val recordA = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordACrn))),
      personKeyEntity = cluster,
    )
    val recordB = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordBCrn))),
      personKeyEntity = cluster,
    )

    mergeRecord(recordB, recordA)
    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("CRN" to recordACrn, "EVENT_TYPE" to OFFENDER_GDPR_DELETION, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordACrn, "UUID" to cluster.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to cluster.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to cluster.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )

    await untilAsserted { assertThat(personRepository.findByCrn(recordACrn)).isNull() }
    await untilAsserted { assertThat(personRepository.findByCrn(recordBCrn)).isNull() }
    await untilAsserted { assertThat(personKeyRepository.findByPersonId(cluster.personId)).isNull() }
  }

  @Test
  fun `should process offender delete with 2 records which have merged on different UUIDs`() {
    val recordACrn = randomCRN()
    val recordBCrn = randomCRN()
    val domainEvent = buildDomainEvent(recordACrn)

    // First Record Cluster (1 Record)
    val clusterA = createPersonKey()
    val mergedTo = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordACrn))),
      personKeyEntity = clusterA,
    )

    // First Record Cluster (1 Record)
    val clusterB = createPersonKey()
    val mergedFrom = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordBCrn))),
      personKeyEntity = clusterB,
    )

    mergeRecord(mergedFrom, mergedTo)
    mergeUuid(clusterB, clusterA)
    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("CRN" to recordACrn, "EVENT_TYPE" to OFFENDER_GDPR_DELETION, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordACrn, "UUID" to clusterA.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to recordACrn, "UUID" to clusterA.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to clusterB.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to clusterB.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )

    await untilAsserted { assertThat(personRepository.findByCrn(recordACrn)).isNull() }
    await untilAsserted { assertThat(personRepository.findByCrn(recordBCrn)).isNull() }
    await untilAsserted { assertThat(personKeyRepository.findByPersonId(clusterA.personId)).isNull() }
    await untilAsserted { assertThat(personKeyRepository.findByPersonId(clusterB.personId)).isNull() }
  }

  @Test
  fun `should process offender delete with 3 records which have merged on different UUIDs`() {
    // Merged Record Chain: C -> B -> A
    val recordACrn = randomCRN()
    val recordBCrn = randomCRN()
    val recordCCrn = randomCRN()
    val domainEvent = buildDomainEvent(recordACrn)

    // First Record Cluster (1 Record)
    val clusterA = createPersonKey()
    val recordA = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordACrn))),
      personKeyEntity = clusterA,
    )

    // Second Record Cluster (2 Records - C merged to B)
    val clusterB = createPersonKey()
    val recordB = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordBCrn))),
      personKeyEntity = clusterB,
    )
    val recordC = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordCCrn))),
    )

    mergeRecord(recordC, recordB)
    mergeRecord(recordB, recordA)
    mergeUuid(clusterB, clusterA)
    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("CRN" to recordACrn, "EVENT_TYPE" to OFFENDER_GDPR_DELETION, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordACrn, "UUID" to clusterA.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to recordACrn, "UUID" to clusterA.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to clusterB.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to clusterB.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )

    await untilAsserted { assertThat(personRepository.findByCrn(recordACrn)).isNull() }
    await untilAsserted { assertThat(personRepository.findByCrn(recordBCrn)).isNull() }
    await untilAsserted { assertThat(personRepository.findByCrn(recordCCrn)).isNull() }
    await untilAsserted { assertThat(personKeyRepository.findByPersonId(clusterA.personId)).isNull() }
    await untilAsserted { assertThat(personKeyRepository.findByPersonId(clusterB.personId)).isNull() }
  }

  @Test
  fun `should process offender delete when multiple records merged on single record`() {
    val recordACrn = randomCRN()
    val recordBCrn = randomCRN()
    val recordCCrn = randomCRN()

    val domainEvent = buildDomainEvent(recordACrn)

    // Record Cluster (3 Records - B -> A <- C)
    val cluster = createPersonKey()
    val recordA = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordACrn))),
      personKeyEntity = cluster,
    )
    val recordB = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordBCrn))),
    )
    val recordC = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordCCrn))),
    )
    mergeRecord(recordB, recordA)
    mergeRecord(recordC, recordA)
    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("CRN" to recordACrn, "EVENT_TYPE" to OFFENDER_GDPR_DELETION, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordACrn, "UUID" to cluster.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to recordACrn, "UUID" to cluster.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to null, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordCCrn, "UUID" to null, "SOURCE_SYSTEM" to "DELIUS"),
    )

    await untilAsserted { assertThat(personRepository.findByCrn(recordACrn)).isNull() }
    await untilAsserted { assertThat(personRepository.findByCrn(recordBCrn)).isNull() }
    await untilAsserted { assertThat(personRepository.findByCrn(recordCCrn)).isNull() }
    await untilAsserted { assertThat(personKeyRepository.findByPersonId(cluster.personId)).isNull() }
  }

  @Test
  fun `should process offender delete when 2 records merged on cluster with other active records`() {
    val recordACrn = randomCRN()
    val recordBCrn = randomCRN()
    val recordCCrn = randomCRN()

    val domainEvent = buildDomainEvent(recordACrn)

    // Record Cluster (3 Records - B -> A)
    val cluster = createPersonKey()
    val recordA = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordACrn))),
      personKeyEntity = cluster,
    )
    val recordB = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordBCrn))),
      personKeyEntity = cluster,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = recordCCrn))),
      personKeyEntity = cluster,
    )
    mergeRecord(recordB, recordA)
    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("CRN" to recordACrn, "EVENT_TYPE" to OFFENDER_GDPR_DELETION, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordACrn, "UUID" to cluster.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to recordBCrn, "UUID" to cluster.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )

    await untilAsserted { assertThat(personRepository.findByCrn(recordACrn)).isNull() }
    await untilAsserted { assertThat(personRepository.findByCrn(recordBCrn)).isNull() }

    val updatedCluster = personKeyRepository.findByPersonId(cluster.personId)
    assertThat(updatedCluster).isNotNull()
    assertThat(updatedCluster?.personEntities?.size).isEqualTo(1)
    assertThat(updatedCluster?.personEntities?.first()?.crn).isEqualTo(recordCCrn)
  }

  private fun buildDomainEvent(crn: String, eventType: String = OFFENDER_GDPR_DELETION): DomainEvent {
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))

    return DomainEvent(
      eventType = eventType,
      personReference = personReference,
      additionalInformation = null,
    )
  }

  @Test
  fun `should process offender delete and map to EventLogging table`() {
    val crn = randomCRN()
    val domainEvent = buildDomainEvent(crn)
    val personKey = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
      personKeyEntity = personKey,
    )
    val personEntity = await.atMost(4, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    val beforeDataDTO = Person.from(personEntity)
    val beforeData = objectMapper.writeValueAsString(beforeDataDTO)

    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    val loggedEvent = await.atMost(4, SECONDS) untilNotNull {
      eventLoggingRepository.findFirstBySourceSystemIdOrderByEventTimestampDesc(crn)
    }

    assertThat(loggedEvent).isNotNull
    assertThat(loggedEvent.eventType).isEqualTo(OFFENDER_GDPR_DELETION)
    assertThat(loggedEvent.sourceSystemId).isEqualTo(crn)
    assertThat(loggedEvent.sourceSystem).isEqualTo(DELIUS.name)
    assertThat(loggedEvent.eventTimestamp).isBefore(LocalDateTime.now())
    assertThat(loggedEvent.beforeData).isEqualTo(beforeData)
    assertThat(loggedEvent.processedData).isEqualTo(null)

    assertThat(loggedEvent.uuid).isEqualTo(personEntity.personKey?.personId.toString())
  }
}
