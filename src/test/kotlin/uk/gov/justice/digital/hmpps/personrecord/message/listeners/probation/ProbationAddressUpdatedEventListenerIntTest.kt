package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode

class ProbationAddressUpdatedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consuming an address updated event - cpr address exists - updates address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode(), deliusAddressId = probationAddress.deliusAddressId))),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubPersonMatchScores()
    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_UPDATED)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()
    assertThat(cprAddressAfterUpdate.id).isEqualTo(cprAddressBeforeUpdate.id)
    assertThat(cprAddressAfterUpdate.updateId).isEqualTo(cprAddressBeforeUpdate.updateId)
    assertAddress(actualPersonEntity, probationAddress)
  }

  @Test
  fun `consuming address updated event - address not retrieved from probation - pushes message to dead letter queue`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode(), deliusAddressId = probationAddress.deliusAddressId))),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubGetRequestToProbation(probationAddress, status = 404)

    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_UPDATED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()
    assertThat(cprAddressAfterUpdate).usingRecursiveComparison().isEqualTo(cprAddressBeforeUpdate)
  }

  @Test
  fun `consuming address updated event - cpr person does not exist - pushes message to dead letter queue`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode(), deliusAddressId = probationAddress.deliusAddressId))),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(randomCrn(), probationAddress.deliusAddressId, OFFENDER_ADDRESS_UPDATED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()
    assertThat(cprAddressAfterUpdate).usingRecursiveComparison().isEqualTo(cprAddressBeforeUpdate)
  }

  @Test
  fun `consuming address updated event - cpr address does not exist - pushes message to dead letter queue`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode(), deliusAddressId = randomDigit().toLong()))),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_UPDATED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()
    assertThat(cprAddressAfterUpdate).usingRecursiveComparison().isEqualTo(cprAddressBeforeUpdate)
  }
}
