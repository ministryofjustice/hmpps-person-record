package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class ProbationAddressCreatedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consuming address created event - saves address`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubPersonMatchScores()
    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(cprPerson.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_CREATED)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(cprPerson.crn!!) }
    assertAddress(actualPersonEntity, probationAddress)
  }

  @Test
  fun `consuming address created event - address with delius address id already exists - does not save address`() {
    val originalProbationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(originalProbationAddress)!!)),
    )
    val addressEntityBeforeCreateEvent = personEntity.addresses.first()
    stubGetRequestToProbation(randomProbationAddress(originalProbationAddress.deliusAddressId))

    publishProbationAddressEvent(personEntity.crn, originalProbationAddress.deliusAddressId, OFFENDER_ADDRESS_CREATED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val addressEntityAfterCreateEvent = actualPersonEntity.addresses.first()
    assertThat(addressEntityAfterCreateEvent.id).isEqualTo(addressEntityBeforeCreateEvent.id)
    assertThat(addressEntityAfterCreateEvent.updateId).isEqualTo(addressEntityBeforeCreateEvent.updateId)
    assertAddress(actualPersonEntity, originalProbationAddress)
  }

  @Test
  fun `consuming address created event - address not retrieved from probation - does not save address`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubGetRequestToProbation(probationAddress, status = 404)
    publishProbationAddressEvent(cprPerson.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_CREATED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)
    assertThat(personRepository.findByCrn(cprPerson.crn!!)!!.addresses.size).isEqualTo(0)
  }

  @Test
  fun `consuming address created event - cpr person does not exist - does not save address`() {
    val probationAddress = randomProbationAddress()
    val cprPerson = createRandomProbationPersonDetails().copy(addresses = emptyList())
    createPersonKey()
      .addPerson(cprPerson)

    stubGetRequestToProbation(probationAddress)
    publishProbationAddressEvent(randomCrn(), probationAddress.deliusAddressId, OFFENDER_ADDRESS_CREATED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)
    assertThat(personRepository.findByCrn(cprPerson.crn!!)!!.addresses.size).isEqualTo(0)
  }
}
