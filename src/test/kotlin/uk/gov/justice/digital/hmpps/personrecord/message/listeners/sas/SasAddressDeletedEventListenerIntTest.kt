package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationEventListenerTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.util.UUID

class SasAddressDeletedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consume sas delete event - address exists - deletes address`() {
    val personEntity = createPerson(createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode()))))
    createPersonKey().addPerson(personEntity)

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    publishDomainEvent(
      SAS_ADDRESS_DELETED,
      DomainEvent(
        eventType = SAS_ADDRESS_DELETED,
        additionalInformation = AdditionalInformation(
          inboundCprAddressId = personEntity.addresses.first().updateId!!.toString(),
        ),
      ),
    )

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
    val personEntity = createPerson(createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode()))))
    createPersonKey().addPerson(personEntity)

    publishDomainEvent(
      SAS_ADDRESS_DELETED,
      DomainEvent(
        eventType = SAS_ADDRESS_DELETED,
        additionalInformation = AdditionalInformation(
          inboundCprAddressId = UUID.randomUUID().toString(),
        ),
      ),
    )

    expectNoMessagesOnQueueOrDlq(sasEventsQueue)
    assertCorrectActionsHappenAfterSasAddressDelete(personEntity.crn!!)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
  }
}
