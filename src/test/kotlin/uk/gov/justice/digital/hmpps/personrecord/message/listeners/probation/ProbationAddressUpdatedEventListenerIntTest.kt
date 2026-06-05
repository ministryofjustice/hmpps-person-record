package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString

class ProbationAddressUpdatedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consuming an address updated event - cpr address exists - updates address`() {
    val originalProbationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(originalProbationAddress)!!)),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    val updatedProbationAddress = randomProbationAddress().copy(deliusAddressId = cprAddressBeforeUpdate.deliusAddressId)
    stubGetRequestToProbation(updatedProbationAddress)

    publishProbationAddressEvent(personEntity.crn, originalProbationAddress.deliusAddressId, OFFENDER_ADDRESS_UPDATED)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()
    assertThat(cprAddressAfterUpdate.id).isEqualTo(cprAddressBeforeUpdate.id)
    assertThat(cprAddressAfterUpdate.updateId).isEqualTo(cprAddressBeforeUpdate.updateId)
    assertAddress(personEntity.crn!!, updatedProbationAddress)
    assertProbationAddressDomainEventPublished(
      expectedEventType = CPR_PROBATION_ADDRESS_UPDATED,
      crn = personEntity.crn!!,
    )
  }

  @Test
  fun `consuming an address updated event - no matching fields updated - does not save in person match or trigger recluster`() {
    val originalProbationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(originalProbationAddress)!!)),
    )

    val updatedProbationAddress = originalProbationAddress.copy(notes = randomLowerCaseString())
    stubGetRequestToProbation(updatedProbationAddress)

    publishProbationAddressEvent(personEntity.crn, updatedProbationAddress.deliusAddressId, OFFENDER_ADDRESS_UPDATED)
    wiremock.verify(0, postRequestedFor(urlEqualTo("/person")))
    wiremock.verify(0, getRequestedFor(urlEqualTo("/person/score/.*")))
  }

  @Test
  fun `consuming address updated event - address not retrieved from probation - does not update address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(probationAddress)!!)),
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
  fun `consuming address updated event - cpr person does not exist - does not update address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address.from(probationAddress)!!)),
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
  fun `consuming address updated event - cpr address does not exist - saves address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(createRandomProbationPersonDetails().copy(addresses = listOf()))

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_UPDATED)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    assertAddress(personEntity.crn!!, probationAddress)
    assertProbationAddressDomainEventPublished(
      expectedEventType = CPR_PROBATION_ADDRESS_CREATED,
      crn = personEntity.crn!!,
    )
  }
}
