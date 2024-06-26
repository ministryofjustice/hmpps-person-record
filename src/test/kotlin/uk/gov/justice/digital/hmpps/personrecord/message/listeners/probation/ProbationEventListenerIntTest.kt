package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DOMAIN_EVENT_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS

class ProbationEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `creates person when when new offender created event is published`() {
    val prisonNumber = randomPrisonNumber()
    val prefix = randomFirstName()
    val pnc = randomPnc()
    val cro = randomCro()
    val crn = probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, pnc, prefix = prefix, prisonNumber = prisonNumber, cro = cro, addresses = listOf(ApiResponseSetupAddress("LS1 1AB"), ApiResponseSetupAddress("M21 9LX")))

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(personEntity.personIdentifier).isNull()
    assertThat(personEntity.firstName).isEqualTo("${prefix}FirstName")
    assertThat(personEntity.middleNames).isEqualTo("PreferredMiddleName")
    assertThat(personEntity.lastName).isEqualTo("${prefix}LastName")
    assertThat(personEntity.title).isEqualTo("Mr")
    assertThat(personEntity.pnc).isEqualTo(PNCIdentifier(pnc))
    assertThat(personEntity.crn).isEqualTo(crn)
    assertThat(personEntity.cro).isEqualTo(CROIdentifier.from(cro))
    assertThat(personEntity.aliases.size).isEqualTo(1)
    assertThat(personEntity.aliases[0].firstName).isEqualTo("${prefix}FirstName")
    assertThat(personEntity.aliases[0].middleNames).isEqualTo("MiddleName")
    assertThat(personEntity.aliases[0].lastName).isEqualTo("${prefix}LastName")
    assertThat(personEntity.aliases[0].dateOfBirth).isEqualTo(LocalDate.of(2024, 5, 30))
    assertThat(personEntity.addresses.size).isEqualTo(2)
    assertThat(personEntity.addresses[0].postcode).isEqualTo("LS1 1AB")
    assertThat(personEntity.addresses[1].postcode).isEqualTo("M21 9LX")
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
    val crn = probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, null)
    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    assertThat(personEntity.pnc?.pncId).isEqualTo("")

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should create two offenders with same prisonNumber but different CRNs`() {
    val prisonNumber: String = randomPrisonNumber()
    val crn = probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, null, prisonNumber = prisonNumber)
    await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))

    val nextCrn = probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, null, prisonNumber = prisonNumber)
    await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(nextCrn) }

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to nextCrn))
  }

  @Test
  fun `should handle new offender details with an empty pnc`() {
    val crn = probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, "")

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }

    assertThat(personEntity.pnc?.pncId).isEqualTo("")

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should not push 404 to dead letter queue but discard message instead`() {
    val crn = randomCRN()
    stub404Response(crn)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationEventsQueue?.sqsClient?.countAllMessagesOnQueue(probationEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(probationEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"), 1)
  }

  @Test
  fun `should retry on 500 error`() {
    val crn = randomCRN()
    stub500Response(crn)
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, pnc = "", crn = crn, scenario = "retry", currentScenarioState = "next request will succeed")

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationEventsQueue?.sqsClient?.countAllMessagesOnQueue(probationEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(probationEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"), 1)
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
  }

  @Test
  fun `should process OFFENDER_DETAILS_CHANGED event successfully`() {
    val pnc = randomPnc()
    val crn = probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, pnc)
    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(personEntity.pnc).isEqualTo(PNCIdentifier(pnc))
    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    val changedPnc = randomPnc()
    probationDomainEventAndResponseSetup("OFFENDER_DETAILS_CHANGED", changedPnc, crn)

    val updatedPersonEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(updatedPersonEntity.pnc).isEqualTo(PNCIdentifier(changedPnc))

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to "OFFENDER_DETAILS_CHANGED", "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
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

  private fun stub500Response(crn: String) {
    wiremock.stubFor(
      WireMock.get("/probation-cases/$crn")
        .inScenario("retry")
        .whenScenarioStateIs(STARTED)
        .willSetStateTo("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }
}
