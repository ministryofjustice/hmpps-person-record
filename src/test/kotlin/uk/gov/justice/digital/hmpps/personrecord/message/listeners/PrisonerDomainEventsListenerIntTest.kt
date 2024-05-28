package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Events.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Events.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.messages.prisonerSearchResponse
import java.util.UUID

class PrisonerDomainEventsListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should receive the message successfully when prisoner created event published`() {
    // Given
    val nomsNumber = UUID.randomUUID().toString()
    stubPrisonerResponse(nomsNumber)

    val additionalInformation = AdditionalInformation(nomsNumber = nomsNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, detailUrl = createNomsDetailUrl(nomsNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(TelemetryEventType.NOMIS_MESSAGE_RECEIVED, mapOf("NOMS_NUMBER" to nomsNumber, "eventType" to PRISONER_CREATED))
  }

  @Test
  fun `should receive the message successfully when prisoner updated event published`() {
    // Given
    val nomsNumber = UUID.randomUUID().toString()
    stubPrisonerResponse(nomsNumber)

    val additionalInformation = AdditionalInformation(nomsNumber = nomsNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, detailUrl = createNomsDetailUrl(nomsNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)

    checkTelemetry(TelemetryEventType.NOMIS_MESSAGE_RECEIVED, mapOf("NOMS_NUMBER" to nomsNumber, "eventType" to PRISONER_UPDATED))
  }

  fun stubPrisonerResponse(nomsNumber: String) {
    wiremock.stubFor(
      WireMock.get("/prisoner/$nomsNumber")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(prisonerSearchResponse(nomsNumber))
        ),
    )
  }
}
