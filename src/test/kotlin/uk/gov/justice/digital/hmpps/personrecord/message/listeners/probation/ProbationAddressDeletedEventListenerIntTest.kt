package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDeliusAddressId

class ProbationAddressDeletedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consuming address deleted event - deletes address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails(),
      configure = addAddressToRecord(Address.from(probationAddress)!!),
    )
    val addressEntity = personEntity.addresses.first()

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    publishProbationAddressDeletedEvent(personEntity.crn, probationAddress.deliusAddressId)

    expectNoMessagesOnQueueOrDlq(probationEventsQueue)
    val actualPersonEntity = personRepository.findByCrn(personEntity.crn!!)!!
    assertThat(actualPersonEntity.addresses.size).isEqualTo(0)

    assertCprAddressDeletedEventPublished(personEntity.crn!!, addressEntity.updateId!!, null, DELIUS)
  }

  @Test
  fun `consuming address deleted event - person with crn does not exist - still deletes address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails(),
      configure = addAddressToRecord(Address.from(probationAddress)!!),
    )
    val addressEntity = personEntity.addresses.first()

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    publishProbationAddressDeletedEvent(randomCrn(), probationAddress.deliusAddressId)

    expectNoMessagesOnQueueOrDlq(probationEventsQueue)

    val actualPersonEntity = personRepository.findByCrn(personEntity.crn!!)!!
    assertThat(actualPersonEntity.addresses.size).isEqualTo(0)

    assertCprAddressDeletedEventPublished(personEntity.crn!!, addressEntity.updateId!!, null, DELIUS)
  }

  @Test
  fun `consuming address deleted event - cpr address does not exist - does not delete any addresses - does not place on dlq`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails(),
      configure = addAddressToRecord(Address.from(probationAddress.copy(deliusAddressId = randomDeliusAddressId()))!!),
    )

    publishProbationAddressDeletedEvent(personEntity.crn, probationAddress.deliusAddressId)

    expectNoMessagesOnQueueOrDlq(probationEventsQueue)
    expectNoMessagesOnQueueOrDlq(testOnlyCPRDomainEventsQueue)
    val actualPersonEntity = personRepository.findByCrn(personEntity.crn!!)!!
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
  }
}
