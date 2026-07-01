package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString

class ProbationAddressUpdatedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consuming an address updated event - cpr address exists - updates address`() {
    val originalProbationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails(),
      configure = addProbationAddress(
        originalProbationAddress,
      ),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    val updatedProbationAddress = randomProbationAddress().copy(deliusAddressId = cprAddressBeforeUpdate.deliusAddressId)
    stubGetRequestToProbation(updatedProbationAddress)

    publishProbationOffenderAddressUpdatedEvent(
      personEntity.crn,
      originalProbationAddress.deliusAddressId,
    )

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()
    assertThat(cprAddressAfterUpdate.id).isEqualTo(cprAddressBeforeUpdate.id)
    assertThat(cprAddressAfterUpdate.updateId).isEqualTo(cprAddressBeforeUpdate.updateId)

    val actualAddress = assertAddress(personEntity.crn!!, updatedProbationAddress)
    assertCprAddressUpdatedEventPublished(personEntity.crn!!, actualAddress.updateId!!, null, DELIUS)
  }

  @Test
  fun `consuming an address updated event - no matching fields updated - does not save in person match or trigger recluster`() {
    val originalProbationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails(),
      configure = addProbationAddress(
        originalProbationAddress,
      ),
    )

    val updatedProbationAddress = originalProbationAddress.copy(notes = randomLowerCaseString())
    stubGetRequestToProbation(updatedProbationAddress)

    publishProbationOffenderAddressUpdatedEvent(personEntity.crn, updatedProbationAddress.deliusAddressId)

    wiremock.verify(0, postRequestedFor(urlEqualTo("/person")))
    wiremock.verify(0, getRequestedFor(urlEqualTo("/person/score/.*")))
  }

  @Test
  fun `consuming address updated event - address not retrieved from probation - does not update address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails(),
      configure = addProbationAddress(
        probationAddress,
      ),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubGetRequestToProbation(probationAddress, status = 404)

    publishProbationOffenderAddressUpdatedEvent(personEntity.crn, probationAddress.deliusAddressId)

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

    publishProbationOffenderAddressUpdatedEvent(personEntity.crn, probationAddress.deliusAddressId)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)

    val actualAddress = assertAddress(personEntity.crn!!, probationAddress)
    assertCprAddressCreatedEventPublished(personEntity.crn!!, actualAddress.updateId!!)
  }
}
