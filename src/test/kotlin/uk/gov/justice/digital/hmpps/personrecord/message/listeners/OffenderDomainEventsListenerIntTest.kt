package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DOMAIN_EVENT_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationCaseResponse
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

const val NEW_OFFENDER_CREATED = "probation-case.engagement.created"

class OffenderDomainEventsListenerIntTest : MessagingMultiNodeTestBase() {

  private val scenarioName: String = "scenario"

  @Test
  fun `should receive the message successfully when new offender event published`() {
    val (crn, expectedPncNumber) = domainEvent(NEW_OFFENDER_CREATED)

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(personEntity.pnc).isEqualTo(expectedPncNumber)
    assertThat(personEntity.crn).isEqualTo(crn)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "eventType" to NEW_OFFENDER_CREATED, "SourceSystem" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should write offender without PNC if PNC is missing`() { // check this is null in response
    val crn = UUID.randomUUID().toString()
    val probationCaseResponseSetup = ProbationCaseResponseSetup(crn = crn, prefix = "POPOne")

    stubSingleResponse(probationCaseResponseSetup, scenarioName, STARTED)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishDomainEvent(NEW_OFFENDER_CREATED, domainEvent)
    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    assertThat(personEntity.pnc?.pncId).isEqualTo("")
    assertThat(personEntity.crn).isEqualTo(crn)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "eventType" to NEW_OFFENDER_CREATED, "SourceSystem" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should handle new offender details with an empty pnc`() {
    val crn = UUID.randomUUID().toString()
    val probationCaseResponseSetup = ProbationCaseResponseSetup(crn = crn, pnc = "", prefix = "POPOne")
    stubSingleResponse(probationCaseResponseSetup, scenarioName, STARTED)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    assertThat(personEntity.pnc?.pncId).isEqualTo("")
    assertThat(personEntity.crn).isEqualTo(crn)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "eventType" to NEW_OFFENDER_CREATED, "SourceSystem" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should not push 404 to dead letter queue`() {
    val crn = UUID.randomUUID().toString()
    stub404Response(crn)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    await.atMost(Duration.ofSeconds(5)) untilCallTo {
      offenderEventsQueue?.sqsClient?.countAllMessagesOnQueue(offenderEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(5)) untilCallTo {
      offenderEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(offenderEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "eventType" to NEW_OFFENDER_CREATED, "SourceSystem" to "DELIUS"))
  }

  @Test
  fun `should receive the message successfully when OFFENDER_ADDRESS_CHANGED`() {
    val (crn, expectedPncNumber) = domainEvent("OFFENDER_ADDRESS_CHANGED")

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(personEntity.pnc).isEqualTo(expectedPncNumber)
    assertThat(personEntity.crn).isEqualTo(crn)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "eventType" to "OFFENDER_ADDRESS_CHANGED", "SourceSystem" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SourceSystem" to "DELIUS", "CRN" to crn))
  }

  private fun domainEvent(eventType: String): Pair<String, PNCIdentifier> {
    val crn = UUID.randomUUID().toString()
    val pnc = "2020/0476873U"
    val probationCaseResponseSetup = ProbationCaseResponseSetup(crn = crn, pnc = pnc, prefix = "POPOne")
    stubSingleResponse(probationCaseResponseSetup, scenarioName, STARTED)
    val expectedPncNumber = PNCIdentifier(pnc)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))

    val domainEvent = DomainEvent(
      eventType = eventType,
      detailUrl = createDeliusDetailUrl(crn),
      personReference = personReference,
      additionalInformation = null,
    )
    publishDomainEvent(eventType, domainEvent)
    return Pair(crn, expectedPncNumber)
  }

  private fun stubSingleResponse(probationCase: ProbationCaseResponseSetup, scenarioName: String, scenarioState: String) {
    wiremock.stubFor(
      WireMock.get("/probation-cases/${probationCase.crn}")
        .inScenario(scenarioName)
        .whenScenarioStateIs(scenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(probationCaseResponse(probationCase))
            .withStatus(200),
        ),
    )
  }

  private fun stub404Response(crn: String) {
    wiremock.stubFor(
      WireMock.get("/probation-cases/$crn")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404),
        ),
    )
  }
}

data class ProbationCaseResponseSetup(val crn: String, val pnc: String? = null, val prefix: String)
