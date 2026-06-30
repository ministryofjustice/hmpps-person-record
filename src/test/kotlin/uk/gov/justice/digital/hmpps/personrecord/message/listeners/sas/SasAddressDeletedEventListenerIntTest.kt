package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationEventListenerTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_DELETED
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
    assertDomainEventPublishedAfterSasEvent(
      expectedEventType = CPR_PROBATION_ADDRESS_DELETED,
      crn = personEntity.crn!!,
      cprAddressUpdateId = personEntity.addresses.first().updateId!!.toString(),
    )
  }

  @Test
  fun `consume sas delete event - cpr address does not exist - does not push message to dlq`() {
    val personEntity = createPerson(createRandomProbationPersonDetails(), configure = addAddressToProbationRecord(Address(postcode = randomPostcode())))
    createPersonKey().addPerson(personEntity)

    publishSasAddressDeletedEvent(UUID.randomUUID())

    expectNoMessagesOnQueueOrDlq(sasEventsQueue)
    assertCorrectActionsHappenAfterSasAddressDelete(personEntity.crn!!)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
  }
}
