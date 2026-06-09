package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit

class ProbationAddressCreatedCircularEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `given an address created event - event contains CPR event source - updates delius address id only`() {
    val crn = randomCrn()
    val addressCreatedBySas = randomProbationAddress().copy(deliusAddressId = null)
    val personEntity = createPerson(createRandomProbationPersonDetails(crn = crn).copy(addresses = listOf(Address.from(addressCreatedBySas)!!)))
    createPersonKey()
      .addPerson(personEntity)
    val addressEntity = personEntity.addresses.first()

    val deliusAddressId = randomDigit().toLong()
    val addressCreatedByDeliusBecauseOfCprEvent = addressCreatedBySas.copy(deliusAddressId = deliusAddressId)
    stubGetRequestToProbation(addressCreatedByDeliusBecauseOfCprEvent)

    assertNull(addressEntity.deliusAddressId)
    publishDomainEvent(
      eventType = OFFENDER_ADDRESS_CREATED,
      domainEvent = DomainEvent(
        eventType = OFFENDER_ADDRESS_CREATED,
        detailUrl = "/address/$deliusAddressId",
        additionalInformation = AdditionalInformation(cprAddressId = addressEntity.updateId.toString(), inboundDeliusAddressId = deliusAddressId.toString()),
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn))),
      ),
      eventSource = DomainEventSource.CPR,
    )

    val actualAddresses = awaitNotNull { personRepository.findByCrn(crn)!!.addresses }
    assertThat(actualAddresses.size).isEqualTo(1)
    val actualAddress = actualAddresses.first()
    assertThat(actualAddress.deliusAddressId).isEqualTo(deliusAddressId)

    assertNoCprActionsHappenAfterAddressPatch(crn)
  }

  private fun assertNoCprActionsHappenAfterAddressPatch(crn: String) {
    // assert no recluster happened or event logs saved
    checkEventLog(crn, CPRLogEvents.CPR_RECORD_CREATED) { assertThat(it).isEmpty() }
    checkEventLog(crn, CPRLogEvents.CPR_RECORD_UPDATED) { assertThat(it).isEmpty() }

    // assert CPR does not publish any domain events
    expectNoMessagesOnQueueOrDlq(testOnlyCPRDomainEventsQueue)

    // assert no telemetry events made
    checkTelemetry(
      event = CPR_RECORD_UPDATED,
      expected = mapOf("SOURCE_SYSTEM" to DELIUS.name, "CRN" to crn),
      times = 0,
    )

    // assert no person match calls made
    wiremock.verify(0, postRequestedFor(urlEqualTo("/person")))
    wiremock.verify(0, getRequestedFor(urlEqualTo("/person/score/.*")))
  }
}
