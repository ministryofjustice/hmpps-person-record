package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString

@ExtendWith(OutputCaptureExtension::class)
class ProbationAddressUpdatedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consuming an address updated event - cpr address exists - updates address`() {
    val originalProbationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails(),
      configure = addAddressToRecord(
        Address.from(originalProbationAddress)!!,
      ),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    val updatedProbationAddress = randomProbationAddress().copy(deliusAddressId = cprAddressBeforeUpdate.deliusAddressId)
    stubGetRequestToProbation(updatedProbationAddress)

    publishProbationAddressUpdatedEvent(
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
      configure = addAddressToRecord(Address.from(originalProbationAddress)!!),
    )

    val updatedProbationAddress = originalProbationAddress.copy(notes = randomLowerCaseString())
    stubGetRequestToProbation(updatedProbationAddress)

    publishProbationAddressUpdatedEvent(personEntity.crn, updatedProbationAddress.deliusAddressId)

    wiremock.verify(0, postRequestedFor(urlEqualTo("/person")))
    wiremock.verify(0, getRequestedFor(urlEqualTo("/person/score/.*")))
  }

  @Test
  fun `consuming address updated event - address not retrieved from probation - does not update address`(output: CapturedOutput) {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails(),
      configure = addAddressToRecord(Address.from(probationAddress)!!),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubGetRequestToProbation(probationAddress, status = 404)

    publishProbationAddressUpdatedEvent(personEntity.crn, probationAddress.deliusAddressId)

    expectNoMessagesOnQueueOrDlq(probationEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()
    assertThat(cprAddressAfterUpdate).usingRecursiveComparison().isEqualTo(cprAddressBeforeUpdate)
    awaitAssert { assertThat(output.all).contains("Discarding message of type probation-case.address.updated due to discardable not found exception") }
  }

  @Test
  fun `consuming address updated event - cpr address does not exist - saves address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(createRandomProbationPersonDetails().copy(addresses = listOf()))

    stubPersonMatchUpsert()
    stubPersonMatchScores()
    stubGetRequestToProbation(probationAddress)

    publishProbationAddressUpdatedEvent(personEntity.crn, probationAddress.deliusAddressId)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)

    val actualAddress = assertAddress(personEntity.crn!!, probationAddress)
    assertCprAddressCreatedEventPublished(personEntity.crn!!, actualAddress.updateId!!)
  }
}
