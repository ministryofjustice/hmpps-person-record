package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DOMAIN_EVENT_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.PRISONER_UPDATED
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

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("NOMS_NUMBER" to nomsNumber, "eventType" to PRISONER_CREATED, "SourceSystem" to "NOMIS"))
  }

  @Test
  fun `should receive the message successfully when prisoner updated event published`() {
    // Given
    val nomsNumber = UUID.randomUUID().toString()
    stubPrisonerResponse(nomsNumber)

    val additionalInformation = AdditionalInformation(nomsNumber = nomsNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, detailUrl = createNomsDetailUrl(nomsNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("NOMS_NUMBER" to nomsNumber, "eventType" to PRISONER_UPDATED, "SourceSystem" to "NOMIS"))
  }

  fun stubPrisonerResponse(nomsNumber: String) {
    wiremock.stubFor(
      WireMock.get("/prisoner/$nomsNumber")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(prisonerSearchResponse(nomsNumber)),
        ),
    )
  }
}
