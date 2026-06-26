package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class ProbationAddressCreatedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consuming address created event - saves address`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    stubGetRequestToProbation(probationAddress)

    publishProbationOffenderAddressCreatedEvent(cprPerson.crn, null, probationAddress.deliusAddressId, DELIUS)

    val actualAddress = assertAddress(cprPerson.crn!!, probationAddress)
    assertDomainEventPublishedAfterDeliusEvent(
      expectedEventType = CPR_PROBATION_ADDRESS_CREATED,
      crn = cprPerson.crn,
      cprAddressUpdateId = actualAddress.updateId.toString(),
    )
  }

  @Test
  fun `consuming address created event - address with delius address id already exists - updates address`() {
    val originalProbationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(originalProbationAddress)!!)),
    )
    val addressEntityBeforeCreateEvent = personEntity.addresses.first()

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    val updatedProbationAddress = randomProbationAddress(originalProbationAddress.deliusAddressId)
    stubGetRequestToProbation(updatedProbationAddress)

    publishProbationOffenderAddressCreatedEvent(personEntity.crn, null, originalProbationAddress.deliusAddressId, DELIUS)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val addressEntityAfterCreateEvent = actualPersonEntity.addresses.first()
    assertThat(addressEntityAfterCreateEvent.id).isEqualTo(addressEntityBeforeCreateEvent.id)
    assertThat(addressEntityAfterCreateEvent.updateId).isEqualTo(addressEntityBeforeCreateEvent.updateId)

    val actualAddress = assertAddress(personEntity.crn!!, updatedProbationAddress)
    assertDomainEventPublishedAfterDeliusEvent(
      expectedEventType = CPR_PROBATION_ADDRESS_UPDATED,
      crn = personEntity.crn!!,
      cprAddressUpdateId = actualAddress.updateId.toString(),
    )
  }

  @Test
  fun `consuming address created event - address not retrieved from probation - does not save address`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubGetRequestToProbation(probationAddress, status = 404)
    publishProbationOffenderAddressCreatedEvent(cprPerson.crn, null, probationAddress.deliusAddressId, DELIUS)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
    assertThat(personRepository.findByCrn(cprPerson.crn!!)!!.addresses.size).isEqualTo(0)
  }

  @Test
  fun `consuming address created event - cpr person does not exist - does not save address`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubGetRequestToProbation(probationAddress)
    publishProbationOffenderAddressCreatedEvent(randomCrn(), null, probationAddress.deliusAddressId, DELIUS)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
    assertThat(personRepository.findByCrn(cprPerson.crn!!)!!.addresses.size).isEqualTo(0)
  }
}
