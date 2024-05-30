package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DOMAIN_EVENT_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationCaseResponse
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

class OffenderDomainEventsListenerIntTest : MessagingMultiNodeTestBase() {

  private val scenarioName: String = "scenario"

  @Test
  fun `should receive the message successfully when new offender event published`() {
    val prisonNumber = UUID.randomUUID().toString()
    val crn = domainEvent(NEW_OFFENDER_CREATED, "2020/0476873U", prisonNumber = prisonNumber)

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(personEntity.firstName).isEqualTo("POPOneFirstName")
    assertThat(personEntity.middleNames).isEqualTo("PreferredMiddleName")
    assertThat(personEntity.lastName).isEqualTo("POPOneLastName")
    assertThat(personEntity.title).isEqualTo("Mr")
    assertThat(personEntity.pnc).isEqualTo(PNCIdentifier("2020/0476873U"))
    assertThat(personEntity.crn).isEqualTo(crn)
    assertThat(personEntity.cro).isEqualTo(CROIdentifier.from("075715/64Q"))
    assertThat(personEntity.prisonNumber).isEqualTo(prisonNumber)
    assertThat(personEntity.nationalInsuranceNumber).isEqualTo("1234567890")
    assertThat(personEntity.aliases.size).isEqualTo(1)
    assertThat(personEntity.aliases[0].firstName).isEqualTo("POPOneFirstName")
    assertThat(personEntity.aliases[0].middleNames).isEqualTo("MiddleName")
    assertThat(personEntity.aliases[0].lastName).isEqualTo("POPOneLastName")
    assertThat(personEntity.aliases[0].dateOfBirth).isEqualTo(LocalDate.of(2024, 5, 30))
    assertThat(personEntity.addresses.size).isEqualTo(1)
    assertThat(personEntity.addresses[0].postcode).isEqualTo("LS1 1AB")
    assertThat(personEntity.contacts.size).isEqualTo(3)
    assertThat(personEntity.contacts[0].contactType).isEqualTo(ContactType.HOME)
    assertThat(personEntity.contacts[0].contactValue).isEqualTo("01234567890")
    assertThat(personEntity.contacts[1].contactType).isEqualTo(ContactType.MOBILE)
    assertThat(personEntity.contacts[1].contactValue).isEqualTo("01234567890")
    assertThat(personEntity.contacts[2].contactType).isEqualTo(ContactType.EMAIL)
    assertThat(personEntity.contacts[2].contactValue).isEqualTo("test@gmail.com")

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should write offender without PNC if PNC is missing`() {
    val crn = domainEvent(NEW_OFFENDER_CREATED, null)
    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    assertThat(personEntity.pnc?.pncId).isEqualTo("")

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should handle new offender details with an empty pnc`() {
    val crn = domainEvent(NEW_OFFENDER_CREATED, "")

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    assertThat(personEntity.pnc?.pncId).isEqualTo("")

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
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
    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
  }

  @Test
  fun `should process OFFENDER_DETAILS_CHANGED event successfully`() {
    val crn = domainEvent(NEW_OFFENDER_CREATED, "2020/0476873U")
    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(personEntity.pnc).isEqualTo(PNCIdentifier("2020/0476873U"))
    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    domainEvent("OFFENDER_DETAILS_CHANGED", "2003/0062845E", crn)

    val updatedPersonEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(updatedPersonEntity.pnc).isEqualTo(PNCIdentifier("2003/0062845E"))

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to "OFFENDER_DETAILS_CHANGED", "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  private fun domainEvent(eventType: String, pnc: String?, crn: String = UUID.randomUUID().toString(), additionalInformation: AdditionalInformation? = null, prisonNumber: String = UUID.randomUUID().toString()): String {
    val probationCaseResponseSetup = ProbationCaseResponseSetup(crn = crn, pnc = pnc, prefix = "POPOne", prisonNumber = prisonNumber)
    stubSingleResponse(probationCaseResponseSetup, scenarioName, STARTED)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))

    val domainEvent = DomainEvent(
      eventType = eventType,
      detailUrl = createDeliusDetailUrl(crn),
      personReference = personReference,
      additionalInformation = additionalInformation,
    )
    publishDomainEvent(eventType, domainEvent)
    return crn
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

data class ProbationCaseResponseSetup(val crn: String, val pnc: String? = null, val prefix: String, val prisonNumber: String)
