package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_UPDATED
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
    publishProbationAddressEvent(
      crn,
      deliusAddressId,
      OFFENDER_ADDRESS_CREATED,
      DomainEventSource.CPR,
      addressEntity.updateId.toString(),
    )

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

    publishProbationAddressEvent(
      personEntity.crn,
      personEntity.addresses[0].deliusAddressId,
      OFFENDER_ADDRESS_UPDATED,
      DomainEventSource.CPR,
      cprAddressBeforeUpdate.updateId.toString(),
    )

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()
    assertThat(cprAddressAfterUpdate.id).isEqualTo(cprAddressBeforeUpdate.id)
    assertThat(cprAddressAfterUpdate.updateId).isEqualTo(cprAddressBeforeUpdate.updateId)

    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)

    assertNoCprActionsHappenAfterAddressPatch(personEntity.crn!!)
  }
}
