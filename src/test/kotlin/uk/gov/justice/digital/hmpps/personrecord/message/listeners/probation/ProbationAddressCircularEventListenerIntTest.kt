package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit

class ProbationAddressCircularEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `given an address created event - event contains CPR event source - updates delius address id only`() {
    val crn = randomCrn()
    val addressCreatedBySas = randomProbationAddress().copy(deliusAddressId = null)
    val personEntity = createPersonWithNewKey(createRandomProbationPersonDetails(crn = crn).copy(addresses = listOf(Address.from(addressCreatedBySas)!!)))
    val addressEntity = personEntity.addresses.first()

    val deliusAddressId = randomDigit().toLong()

    assertNull(addressEntity.deliusAddressId)

    publishProbationAddressCreatedEvent(
      crn = crn,
      cprAddressId = addressEntity.updateId.toString(),
      deliusAddressId = deliusAddressId,
      eventSource = DomainEventSource.CPR,
    )

//    publishDomainEvent(
//      ProbationOffenderAddressCreatedUpdated(
//        eventType = OFFENDER_ADDRESS_CREATED,
//        occurredAt = Instant.now().asStringWithUkZone(),
//        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn))),
//        additionalInformation = ProbationOffenderAddressCreatedUpdatedInfo(
//          cprAddressId = addressEntity.updateId.toString(),
//          deliusAddressId = deliusAddressId
//        )
//      )
//    )

//    publishProbationAddressEvent(
//      crn,
//      deliusAddressId,
//      OFFENDER_ADDRESS_CREATED,
//      DomainEventSource.CPR,
//      addressEntity.updateId.toString(),
//    )

    awaitAssert {
      assertThat(personRepository.findByCrn(crn)?.addresses?.firstOrNull()?.deliusAddressId).isEqualTo(deliusAddressId)
    }

    assertNoCprActionsHappenAfterAddressPatch(crn)
  }

  @Test
  fun `consuming address updated event - cpr event source - does not update address or publish subsequent events`() {
    val originalProbationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(originalProbationAddress)!!)),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    publishProbationAddressUpdatedEvent(
      crn = personEntity.crn,
      cprAddressId = cprAddressBeforeUpdate.updateId.toString(),
      deliusAddressId = personEntity.addresses[0].deliusAddressId,
      eventSource = DomainEventSource.CPR,
    )

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()
    assertThat(cprAddressAfterUpdate.id).isEqualTo(cprAddressBeforeUpdate.id)
    assertThat(cprAddressAfterUpdate.updateId).isEqualTo(cprAddressBeforeUpdate.updateId)

    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)

    assertNoCprActionsHappenAfterAddressPatch(personEntity.crn!!)
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
    wiremock.verify(0, getRequestedFor(urlEqualTo("/address/*")))
  }
}
