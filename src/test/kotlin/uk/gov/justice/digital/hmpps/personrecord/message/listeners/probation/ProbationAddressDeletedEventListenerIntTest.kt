package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit

class ProbationAddressDeletedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consuming address deleted event - deletes address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(probationAddress)!!)),
    )

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_DELETED)

    expectNoMessagesOnQueueOrDlq(probationEventsQueue)
    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val actualPersonEntity = personRepository.findByCrn(personEntity.crn!!)!!
    assertThat(actualPersonEntity.addresses.size).isEqualTo(0)
  }

  @Test
  fun `consuming address deleted event - person with crn does not exist - still deletes address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(probationAddress)!!)),
    )

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    publishProbationAddressEvent(randomCrn(), probationAddress.deliusAddressId, OFFENDER_ADDRESS_DELETED)

    expectNoMessagesOnQueueOrDlq(probationEventsQueue)
    expectOneMessageOn(testOnlyCPRDomainEventsQueue)

    val actualPersonEntity = personRepository.findByCrn(personEntity.crn!!)!!
    assertThat(actualPersonEntity.addresses.size).isEqualTo(0)
  }

  @Test
  fun `consuming address deleted event - cpr address does not exist - does not delete any addresses - does not place on dlq`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(probationAddress.copy(deliusAddressId = randomDigit().toLong()))!!)),
    )

    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_DELETED)

    expectNoMessagesOnQueueOrDlq(probationEventsQueue)
    expectNoMessagesOnQueueOrDlq(testOnlyCPRDomainEventsQueue)
    val actualPersonEntity = personRepository.findByCrn(personEntity.crn!!)!!
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
  }
}
