package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationEventListenerTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.util.UUID

class SasAddressDeletedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consume sas delete event - address exists - deletes address`() {
    val personEntity = createPerson(createRandomProbationPersonDetails(), configure = addAddressToProbationRecord(Address(postcode = randomPostcode())))
    createPersonKey().addPerson(personEntity)

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    publishSasAddressDeletedEvent(personEntity.addresses.first().updateId!!)

    expectNoMessagesOnQueueOrDlq(sasEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(0)
    val actualAddress = personEntity.addresses.first()
    assertCprAddressDeletedEventPublished(personEntity.crn!!, actualAddress.updateId!!, actualAddress.deliusAddressId, CPR)
  }

  @Test
  fun `consume sas delete event - cpr address does not exist - does not push message to dlq`() {
    val personEntity = createPerson(createRandomProbationPersonDetails(), configure = addAddressToProbationRecord(Address(postcode = randomPostcode())))
    createPersonKey().addPerson(personEntity)

    publishSasAddressDeletedEvent(UUID.randomUUID())

    expectNoMessagesOnQueueOrDlq(sasEventsQueue)
    assertNoCprActions(personEntity.crn!!)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
  }
}
