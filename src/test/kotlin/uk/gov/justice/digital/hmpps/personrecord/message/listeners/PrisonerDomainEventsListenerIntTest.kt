package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NOMIS_CREATE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NOMIS_UPDATE_MESSAGE_RECEIVED
import java.util.UUID

class PrisonerDomainEventsListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should receive the message successfully when prisoner created event published`() {
    // Given
    val nomsNumber = UUID.randomUUID().toString()

    val additionalInformation = AdditionalInformation(nomsNumber = nomsNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, detailUrl = createNomsDetailUrl(nomsNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(NOMIS_CREATE_MESSAGE_RECEIVED, mapOf("NOMS_NUMBER" to nomsNumber))
  }

  @Test
  fun `should receive the message successfully when prisoner updated event published`() {
    // Given
    val nomsNumber = UUID.randomUUID().toString()

    val additionalInformation = AdditionalInformation(nomsNumber = nomsNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, detailUrl = createNomsDetailUrl(nomsNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)

    checkTelemetry(NOMIS_UPDATE_MESSAGE_RECEIVED, mapOf("NOMS_NUMBER" to nomsNumber))
  }
}
