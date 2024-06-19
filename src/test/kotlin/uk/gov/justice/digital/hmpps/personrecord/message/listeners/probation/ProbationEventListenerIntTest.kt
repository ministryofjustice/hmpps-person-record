package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.client.WireMock
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS

class ProbationEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `creates person when when new offender created event is published`() {
    val prisonNumber = randomPrisonNumber()
    val crn = probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, "2020/0476873U", prisonNumber = prisonNumber)

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(personEntity.firstName).isEqualTo("POPOneFirstName")
    assertThat(personEntity.middleNames).isEqualTo("PreferredMiddleName")
    assertThat(personEntity.lastName).isEqualTo("POPOneLastName")
    assertThat(personEntity.title).isEqualTo("Mr")
    assertThat(personEntity.pnc).isEqualTo(PNCIdentifier("2020/0476873U"))
    assertThat(personEntity.crn).isEqualTo(crn)
    assertThat(personEntity.cro).isEqualTo(CROIdentifier.from("075715/64Q"))
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
  fun `should not push 404 to dead letter queue`() {
    val crn = randomCRN()
    stub404Response(crn)

    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    await.atMost(Duration.ofSeconds(5)) untilCallTo {
      probationEventsQueue?.sqsClient?.countAllMessagesOnQueue(probationEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(5)) untilCallTo {
      probationEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(probationEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
  }

  @Test
  fun `should process OFFENDER_DETAILS_CHANGED event successfully`() {
    val crn = probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, "2020/0476873U")
    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(personEntity.pnc).isEqualTo(PNCIdentifier("2020/0476873U"))
    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"))
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn))
    probationDomainEventAndResponseSetup("OFFENDER_DETAILS_CHANGED", "2003/0062845E", crn)

    val updatedPersonEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByCrn(crn) }
    assertThat(updatedPersonEntity.pnc).isEqualTo(PNCIdentifier("2003/0062845E"))

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
}
