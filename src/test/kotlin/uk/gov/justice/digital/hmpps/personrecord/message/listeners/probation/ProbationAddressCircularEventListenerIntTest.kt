package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
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
    publishProbationOffenderAddressCreatedEvent(crn, addressEntity.updateId, deliusAddressId, CPR)

    awaitAssert {
      assertThat(personRepository.findByCrn(crn)?.addresses?.firstOrNull()?.deliusAddressId).isEqualTo(deliusAddressId)
    }

    expectNoMessagesOnQueueOrDlq(testOnlyCPRDomainEventsQueue)
    assertNoCprActions(crn)
  }

  @Test
  fun `consuming address updated event - cpr event source - does not update address or publish subsequent events`() {
    val originalProbationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(originalProbationAddress)!!)),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    publishProbationOffenderAddressCreatedEvent(personEntity.crn, cprAddressBeforeUpdate.updateId, personEntity.addresses[0].deliusAddressId, CPR)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()
    assertThat(cprAddressAfterUpdate.id).isEqualTo(cprAddressBeforeUpdate.id)
    assertThat(cprAddressAfterUpdate.updateId).isEqualTo(cprAddressBeforeUpdate.updateId)

    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)

    expectNoMessagesOnQueueOrDlq(testOnlyCPRDomainEventsQueue)
    assertNoCprActions(personEntity.crn!!)
  }
}
