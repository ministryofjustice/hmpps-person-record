package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
