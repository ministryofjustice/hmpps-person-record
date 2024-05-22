package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.delius.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MULTIPLE_RECORDS_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DELIUS_RECORD_CREATION_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NOMIS_CREATE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NOMIS_UPDATE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.newProbationRecord
import uk.gov.justice.digital.hmpps.personrecord.test.messages.notFoundResponse
import uk.gov.justice.digital.hmpps.personrecord.test.messages.nullPnc
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

class OffenderDomainEventsListenerIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepository

  private fun probationDomainCreatedEventUrl(crn: String) = "/probation-case.engagement.created/$crn"

  @Test
  fun `should receive the message successfully when new offender event published`() {
    // Given
    val crn = UUID.randomUUID().toString()
    val pnc = "2020/0476873U"
    patchRequest(probationDomainCreatedEventUrl(crn), newProbationRecord(crn, pnc))
    val expectedPncNumber = PNCIdentifier.from(pnc)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(personEntity.pnc).isEqualTo(expectedPncNumber)
    assertThat(personEntity.crn).isEqualTo(crn)

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should handle multiple records with same crn, updates first`() {
    val crn = UUID.randomUUID().toString()
    patchRequest(probationDomainCreatedEventUrl(crn), newProbationRecord(crn))

    personRepository.saveAndFlush(
      PersonEntity.from(
        Person.from(
          DeliusOffenderDetail(
            name = Name(surname = "Smith"),
            identifiers = Identifiers(crn = crn),
          ),
        ),
      ),
    )
    personRepository.saveAndFlush(
      PersonEntity.from(
        Person.from(
          DeliusOffenderDetail(
            name = Name(surname = "Smith"),
            identifiers = Identifiers(crn = crn),
          ),
        ),
      ),
    )

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    val personEntities = await.atMost(10, SECONDS) untilNotNull { personRepository.findAllByCrn(crn) }
    assertThat(personEntities.size).isEqualTo(2)
    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))

    checkTelemetry(CPR_MULTIPLE_RECORDS_FOUND, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))

    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should write offender without PNC if PNC is missing`() {
    val crn = UUID.randomUUID().toString()
    patchRequest(probationDomainCreatedEventUrl(crn), newProbationRecord(crn, pnc = null))

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)
    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    assertThat(personEntity.pnc?.pncId).isEqualTo("")
    assertThat(personEntity.crn).isEqualTo(crn)

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should receive the message successfully when prisoner created event published`() {
    // Given
    val nomsNumber = UUID.randomUUID().toString()

    val additionalInformation = AdditionalInformation(nomsNumber = nomsNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, detailUrl = createNomsDetailUrl(nomsNumber), personReference = null, additionalInformation = additionalInformation)
    publishOffenderDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(NOMIS_CREATE_MESSAGE_RECEIVED, mapOf("NOMS_NUMBER" to nomsNumber))
  }

  @Test
  fun `should receive the message successfully when prisoner updated event published`() {
    // Given
    val nomsNumber = UUID.randomUUID().toString()

    val additionalInformation = AdditionalInformation(nomsNumber = nomsNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, detailUrl = createNomsDetailUrl(nomsNumber), personReference = null, additionalInformation = additionalInformation)
    publishOffenderDomainEvent(PRISONER_UPDATED, domainEvent)

    checkTelemetry(NOMIS_UPDATE_MESSAGE_RECEIVED, mapOf("NOMS_NUMBER" to nomsNumber))
  }

  @Test
  fun `should handle new offender details with null pnc`() {
    val crn = UUID.randomUUID().toString()
    patchRequest(probationDomainCreatedEventUrl(crn), nullPnc(crn))

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))

    assertThat(personEntity.pnc?.pncId).isEqualTo("")
    assertThat(personEntity.crn).isEqualTo(crn)
  }

  @Test
  fun `should handle new offender details with an empty pnc`() {
    val crn = UUID.randomUUID().toString()
    patchRequest(probationDomainCreatedEventUrl(crn), newProbationRecord(crn, pnc = ""))

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    assertThat(personEntity.pnc?.pncId).isEqualTo("")
    assertThat(personEntity.crn).isEqualTo(crn)

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should not push 404 to dead letter queue`() {
    // Given
    val crn = UUID.randomUUID().toString()
    patchRequest(probationDomainCreatedEventUrl(crn), notFoundResponse(), 404)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    await.atMost(Duration.ofSeconds(5)) untilCallTo {
      offenderEventsQueue?.sqsClient?.countAllMessagesOnQueue(offenderEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(5)) untilCallTo {
      offenderEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(offenderEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
  }
}
