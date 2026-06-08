package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit

class ProbationAddressCreatedCircularEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `given an address created event - event contains CRP event source - updates delius address id only`() {
    val crn = randomCrn()
    val addressCreatedBySas = randomProbationAddress().copy(deliusAddressId = null)
    val personEntity = createPerson(createRandomProbationPersonDetails(crn = crn).copy(addresses = listOf(Address.from(addressCreatedBySas)!!)))
    createPersonKey()
      .addPerson(personEntity)
    val addressEntity = personEntity.addresses.first()
    assertNull(addressEntity.deliusAddressId)

    val deliusAddressId = randomDigit().toLong()
    val addressCreatedByDelius = addressCreatedBySas.copy(deliusAddressId = deliusAddressId)
    stubGetRequestToProbation(addressCreatedByDelius)

    publishDomainEvent(
      OFFENDER_ADDRESS_CREATED,
      DomainEvent(
        eventType = OFFENDER_ADDRESS_CREATED,
        detailUrl = "/address/$deliusAddressId",
        additionalInformation = AdditionalInformation(cprAddressId = addressEntity.updateId.toString(), inboundDeliusAddressId = deliusAddressId.toString()),
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn))),
      ),
    )

    val actualAddresses = awaitNotNull { personRepository.findByCrn(crn)!!.addresses }
    assertThat(actualAddresses.size).isEqualTo(1)
    val actualAddress = actualAddresses.first()
    assertThat(actualAddress.deliusAddressId).isEqualTo(deliusAddressId)

    // assert no other address fields changed?
    // assert no recluster happened
    // assert no person match calls made
    // assert not domain events emitted
    // assert no event logs saved
    // assert no telemetry events made
  }
}
