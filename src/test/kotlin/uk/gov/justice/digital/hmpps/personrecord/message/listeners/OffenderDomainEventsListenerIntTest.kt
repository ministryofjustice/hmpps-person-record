package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.message.processors.delius.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MULTIPLE_RECORDS_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DELIUS_RECORD_CREATION_RECEIVED
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration
import java.util.concurrent.TimeUnit.SECONDS

class OffenderDomainEventsListenerIntTest : IntegrationTestBase() {

  @Test
  fun `should receive the message successfully when new offender event published`() {
    // Given
    val crn = "XXX1234"
    val expectedPncNumber = PNCIdentifier.from("2020/0476873U")

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(personEntity.pnc).isEqualTo(expectedPncNumber)
    assertThat(personEntity.crn).isEqualTo(crn)
  }

  @Test
  fun `should handle multiple records with same crn, updates first`() {
    // Given
    val crn = "XXX1234"
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

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))

    checkTelemetry(CPR_MULTIPLE_RECORDS_FOUND, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))

    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))

    val personEntities = await.atMost(10, SECONDS) untilNotNull { personRepository.findAllByCrn(crn) }
    assertThat(personEntities.size).isEqualTo(2)
  }

  @Test
  fun `should write offender without PNC if PNC is missing`() {
    val crn = "XXX5678"
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    assertThat(personEntity.pnc?.pncId).isEqualTo("")
    assertThat(personEntity.crn).isEqualTo(crn)
  }

  @Test
  fun `should receive the message successfully when prisoner created event published`() {
    // Given
    val nomsNumber = "A9404DZ"

    val additionalInformation = AdditionalInformation(nomsNumber = nomsNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, detailUrl = createNomsDetailUrl(nomsNumber), personReference = null, additionalInformation = additionalInformation)
    publishOffenderDomainEvent(PRISONER_CREATED, domainEvent)

    await untilAsserted {
      verify(prisonerCreatedEventProcessor).processEvent(
        eq(domainEvent),
      )
    }

    await untilAsserted {
      verify(prisonerDomainEventService).processEvent(
        eq(domainEvent),
      )
    }
  }

  @Test
  fun `should receive the message successfully when prisoner updated event published`() {
    // Given
    val nomsNumber = "B8704DZ"

    val additionalInformation = AdditionalInformation(nomsNumber = nomsNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, detailUrl = createNomsDetailUrl(nomsNumber), personReference = null, additionalInformation = additionalInformation)
    publishOffenderDomainEvent(PRISONER_UPDATED, domainEvent)

    await untilAsserted {
      verify(prisonerUpdatedEventProcessor).processEvent(
        eq(domainEvent),
      )
    }

    await untilAsserted {
      verify(prisonerDomainEventService).processEvent(
        eq(domainEvent),
      )
    }
  }

  @Test
  fun `should handle new offender details with null pnc`() {
    val crn = "E610461"
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    assertThat(personEntity.pnc?.pncId).isEqualTo("")
    assertThat(personEntity.crn).isEqualTo(crn)
  }

  @Test
  fun `should handle new offender details with an empty pnc`() {
    val crn = "E610462"
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    assertThat(personEntity.pnc?.pncId).isEqualTo("")
    assertThat(personEntity.crn).isEqualTo(crn)
  }

  @Test
  fun `should not push 404 to dead letter queue`() {
    // Given
    val crn = "C4321"

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))

    await.atMost(Duration.ofSeconds(5)) untilCallTo {
      cprCourtCaseEventsQueue?.sqsClient?.countAllMessagesOnQueue(cprCourtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(5)) untilCallTo {
      cprCourtCaseEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(cprCourtCaseEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
  }
}
