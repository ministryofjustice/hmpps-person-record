package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit

class ProbationAddressDeletedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consuming address deleted event - deletes address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf()),
    )
    insertAddress(personEntity, probationAddress.deliusAddressId)

    stubPersonMatchScores()
    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_DELETED)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(0)
  }

  @Test
  fun `consuming address deleted event - address not retrieved from probation - does not delete address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf()),
    )
    insertAddress(personEntity, probationAddress.deliusAddressId)

    stubGetRequestToProbation(probationAddress, status = 404)

    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_DELETED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
  }

  @Test
  fun `consuming address deleted event - cpr person does not exist - does not delete address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf()),
    )
    insertAddress(personEntity, probationAddress.deliusAddressId)

    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(randomCrn(), probationAddress.deliusAddressId, OFFENDER_ADDRESS_DELETED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
  }

  @Test
  fun `consuming address deleted event - cpr address does not exist - does not delete any address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf()),
    )
    insertAddress(personEntity, randomDigit().toLong())

    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_DELETED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
  }
}
