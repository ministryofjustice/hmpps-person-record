package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.IDs
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DELIUS_RECORD_CREATION_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NEW_DELIUS_RECORD_NEW_PNC
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration
import java.time.LocalDate
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
    checkTelemetry(NEW_DELIUS_RECORD_NEW_PNC, mapOf("CRN" to crn))

    val offenderEntity = await.atMost(10, SECONDS) untilNotNull { offenderRepository.findByCrn(crn) }
    assertThat(offenderEntity.pncNumber).isEqualTo(expectedPncNumber)
    assertThat(offenderEntity.crn).isEqualTo(crn)
  }

  @Test
  fun `should not create offender when matching multiple crn`() {
    // Given
    val crn = "XXX1234"
    val expectedPncNumber = PNCIdentifier.from("2020/0476873U")

    val offenderEntity1 = OffenderEntity.from(
      OffenderDetail(
        offenderId = 1234567,
        dateOfBirth = LocalDate.now(),
        firstName = "Test",
        otherIds = IDs(
          crn = crn,
          pncNumber = expectedPncNumber.pncId,
        ),
        surname = "Test",
      ),
    )
    offenderRepository.saveAndFlush(offenderEntity1)

    val offenderEntity2 = OffenderEntity.from(
      OffenderDetail(
        offenderId = 1234568,
        dateOfBirth = LocalDate.now(),
        firstName = "Test1",
        otherIds = IDs(
          crn = crn,
          pncNumber = expectedPncNumber.pncId,
        ),
        surname = "Test1",
      ),
    )
    offenderRepository.saveAndFlush(offenderEntity2)

    assertThat(offenderRepository.findAllByCrn(crn)?.size).isEqualTo(2)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(
      eventType = NEW_OFFENDER_CREATED,
      detailUrl = createDeliusDetailUrl(crn),
      personReference = personReference,
      additionalInformation = null,
    )
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
    checkTelemetry(NEW_DELIUS_RECORD_NEW_PNC, emptyMap(), never())
  }

  @Test
  fun `should write offender without PNC if PNC is invalid`() {
    val crn = "XXX5678"
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
    checkTelemetry(NEW_DELIUS_RECORD_NEW_PNC, mapOf("CRN" to crn))

    val offenderEntity = await.atMost(10, SECONDS) untilNotNull { offenderRepository.findByCrn(crn) }

    assertThat(offenderEntity.pncNumber?.pncId).isEqualTo("")
    assertThat(offenderEntity.crn).isEqualTo(crn)
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
    checkTelemetry(NEW_DELIUS_RECORD_NEW_PNC, mapOf("CRN" to crn))

    val offenderEntity = await.atMost(10, SECONDS) untilNotNull { offenderRepository.findByCrn(crn) }

    assertThat(offenderEntity.pncNumber?.pncId).isEqualTo("")
    assertThat(offenderEntity.crn).isEqualTo(crn)
  }

  @Test
  fun `should handle new offender details with an empty pnc`() {
    val crn = "E610462"
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    checkTelemetry(DELIUS_RECORD_CREATION_RECEIVED, mapOf("CRN" to crn))
    checkTelemetry(NEW_DELIUS_RECORD_NEW_PNC, mapOf("CRN" to crn))

    val offenderEntity = await.atMost(10, SECONDS) untilNotNull { offenderRepository.findByCrn(crn) }

    assertThat(offenderEntity.pncNumber?.pncId).isEqualTo("")
    assertThat(offenderEntity.crn).isEqualTo(crn)
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
