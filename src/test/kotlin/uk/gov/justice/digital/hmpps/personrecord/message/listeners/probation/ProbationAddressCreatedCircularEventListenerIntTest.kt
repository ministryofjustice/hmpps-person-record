package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
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

    awaitAssert {
      assertThat(personRepository.findByCrn(crn)?.addresses?.firstOrNull()?.deliusAddressId).isEqualTo(deliusAddressId)
    }

    assertNoCprActionsHappenAfterAddressPatch(crn)
  }

  private fun assertNoCprActionsHappenAfterAddressPatch(crn: String) {
    // assert no recluster happened or event logs saved
    checkEventLog(crn, CPRLogEvents.CPR_RECORD_CREATED) { assertThat(it).isEmpty() }
    checkEventLog(crn, CPRLogEvents.CPR_RECORD_CREATED) { assertThat(it).isEmpty() }
    // assert CPR does not publish any domain events
    expectNoMessagesOnQueueOrDlq(testOnlyCPRDomainEventsQueue)
  }
}
