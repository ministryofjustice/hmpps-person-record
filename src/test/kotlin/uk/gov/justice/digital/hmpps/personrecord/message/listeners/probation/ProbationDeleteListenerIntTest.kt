package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_GDPR_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import java.util.concurrent.TimeUnit

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

    await.atMost(10, TimeUnit.SECONDS) untilAsserted { assertThat(personRepository.findByCrn(crn)).isNull() }

    val uuidSearch = personKeyRepository.findByPersonId(personKey.personId)
    assertThat(uuidSearch).isNotNull()
    assertThat(uuidSearch?.personEntities?.size).isEqualTo(1)
    assertThat(uuidSearch?.personEntities?.first()?.crn).isNotEqualTo(crn)
  }

  @Test
  fun `should process offender delete with 2 records which have merged on a single UUID`() {
    val mergedToCrn = randomCRN()
    val mergedFromCrn = randomCRN()
    val domainEvent = buildDomainEvent(mergedToCrn)
    val personKey = createPersonKey()
    val mergedTo = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = mergedToCrn))),
      personKeyEntity = personKey,
    )
    val mergedFrom = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = mergedFromCrn))),
      personKeyEntity = personKey,
    )
    mergeRecord(mergedFrom, mergedTo)
    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("CRN" to mergedToCrn, "EVENT_TYPE" to OFFENDER_GDPR_DELETION, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to mergedToCrn, "UUID" to personKey.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to mergedFromCrn, "UUID" to personKey.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to mergedFromCrn, "UUID" to personKey.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )

    await untilAsserted { assertThat(personRepository.findByCrn(mergedToCrn)).isNull() }
    await untilAsserted { assertThat(personRepository.findByCrn(mergedFromCrn)).isNull() }
    await untilAsserted { assertThat(personKeyRepository.findByPersonId(personKey.personId)).isNull() }
  }

  @Test
  fun `should process offender delete with 2 records which have merged on different UUIDs`() {
    val mergedToCrn = randomCRN()
    val mergedFromCrn = randomCRN()
    val domainEvent = buildDomainEvent(mergedToCrn)
    val mergedToPersonKey = createPersonKey()
    val mergedTo = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = mergedToCrn))),
      personKeyEntity = mergedToPersonKey,
    )
    val mergedFromPersonKey = createPersonKey()
    val mergedFrom = createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = mergedFromCrn))),
      personKeyEntity = mergedFromPersonKey,
    )
    mergeRecord(mergedFrom, mergedTo)
    mergeUuid(mergedFromPersonKey, mergedToPersonKey)
    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("CRN" to mergedToCrn, "EVENT_TYPE" to OFFENDER_GDPR_DELETION, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to mergedToCrn, "UUID" to mergedToPersonKey.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to mergedToCrn, "UUID" to mergedToPersonKey.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to mergedFromCrn, "UUID" to mergedFromPersonKey.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_UUID_DELETED,
      mapOf("CRN" to mergedFromCrn, "UUID" to mergedFromPersonKey.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )

    await untilAsserted { assertThat(personRepository.findByCrn(mergedToCrn)).isNull() }
    await untilAsserted { assertThat(personRepository.findByCrn(mergedFromCrn)).isNull() }
    await untilAsserted { assertThat(personKeyRepository.findByPersonId(mergedToPersonKey.personId)).isNull() }
    await untilAsserted { assertThat(personKeyRepository.findByPersonId(mergedFromPersonKey.personId)).isNull() }
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
}
