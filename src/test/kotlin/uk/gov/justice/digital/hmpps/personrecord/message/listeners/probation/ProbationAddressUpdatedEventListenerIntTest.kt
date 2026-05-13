package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode

class ProbationAddressUpdatedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `consuming an address updated event - address exists - updates address`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode(), deliusAddressId = probationAddress.deliusAddressId))),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubPersonMatchScores()
    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_UPDATED)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()

    assertThat(cprAddressAfterUpdate.id).isEqualTo(cprAddressBeforeUpdate.id)
    assertThat(cprAddressAfterUpdate.updateId).isEqualTo(cprAddressBeforeUpdate.updateId)
    assertThat(cprAddressAfterUpdate.noFixedAbode).isEqualTo(probationAddress.noFixedAbode)
    assertThat(cprAddressAfterUpdate.startDate).isEqualTo(probationAddress.startDate)
    assertThat(cprAddressAfterUpdate.endDate).isEqualTo(probationAddress.endDate)
    assertThat(cprAddressAfterUpdate.postcode).isEqualTo(probationAddress.postcode)
    assertThat(cprAddressAfterUpdate.fullAddress).isEqualTo(probationAddress.fullAddress)
    assertThat(cprAddressAfterUpdate.buildingName).isEqualTo(probationAddress.buildingName)
    assertThat(cprAddressAfterUpdate.postTown).isEqualTo(probationAddress.townCity)
    assertThat(cprAddressAfterUpdate.county).isEqualTo(probationAddress.county)
    assertThat(cprAddressAfterUpdate.uprn).isEqualTo(probationAddress.uprn)
    assertThat(cprAddressAfterUpdate.deliusAddressId).isEqualTo(probationAddress.deliusAddressId)
    assertThat(cprAddressAfterUpdate.isVerified).isEqualTo(probationAddress.isVerified)
    assertThat(cprAddressAfterUpdate.statusCode!!.name).isEqualTo(probationAddress.status!!.code)
    assertThat(cprAddressAfterUpdate.buildingNumber).isEqualTo(probationAddress.addressNumber)
    assertThat(cprAddressAfterUpdate.thoroughfareName).isEqualTo(probationAddress.streetName)
    assertThat(cprAddressAfterUpdate.dependentLocality).isEqualTo(probationAddress.district)
    assertThat(cprAddressAfterUpdate.comment).isEqualTo(probationAddress.notes)
    assertThat(cprAddressAfterUpdate.usages.first().usageCode.name).isEqualTo(probationAddress.usage!!.code)
    assertThat(cprAddressAfterUpdate.contacts.first().contactType).isEqualTo(ContactType.HOME)
    assertThat(cprAddressAfterUpdate.contacts.first().contactValue).isEqualTo(probationAddress.telephoneNumber)
  }

  @Test
  fun `consuming address updated event - address not retrieved from probation - pushes message to dead letter queue`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode(), deliusAddressId = probationAddress.deliusAddressId))),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubGetRequestToProbation(probationAddress, status = 404)

    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_UPDATED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()

    assertThat(cprAddressAfterUpdate.id).isEqualTo(cprAddressBeforeUpdate.id)
    assertThat(cprAddressAfterUpdate.updateId).isEqualTo(cprAddressBeforeUpdate.updateId)
    assertThat(cprAddressAfterUpdate.noFixedAbode).isEqualTo(cprAddressBeforeUpdate.noFixedAbode)
    assertThat(cprAddressAfterUpdate.startDate).isEqualTo(cprAddressBeforeUpdate.startDate)
    assertThat(cprAddressAfterUpdate.endDate).isEqualTo(cprAddressBeforeUpdate.endDate)
    assertThat(cprAddressAfterUpdate.postcode).isEqualTo(cprAddressBeforeUpdate.postcode)
    assertThat(cprAddressAfterUpdate.fullAddress).isEqualTo(cprAddressBeforeUpdate.fullAddress)
    assertThat(cprAddressAfterUpdate.buildingName).isEqualTo(cprAddressBeforeUpdate.buildingName)
    assertThat(cprAddressAfterUpdate.postTown).isEqualTo(cprAddressBeforeUpdate.postTown)
    assertThat(cprAddressAfterUpdate.county).isEqualTo(cprAddressBeforeUpdate.county)
    assertThat(cprAddressAfterUpdate.uprn).isEqualTo(cprAddressBeforeUpdate.uprn)
    assertThat(cprAddressAfterUpdate.deliusAddressId).isEqualTo(cprAddressBeforeUpdate.deliusAddressId)
    assertThat(cprAddressAfterUpdate.isVerified).isEqualTo(cprAddressBeforeUpdate.isVerified)
    assertThat(cprAddressAfterUpdate.statusCode).isEqualTo(cprAddressBeforeUpdate.statusCode)
    assertThat(cprAddressAfterUpdate.buildingNumber).isEqualTo(cprAddressBeforeUpdate.buildingNumber)
    assertThat(cprAddressAfterUpdate.thoroughfareName).isEqualTo(cprAddressBeforeUpdate.thoroughfareName)
    assertThat(cprAddressAfterUpdate.dependentLocality).isEqualTo(cprAddressBeforeUpdate.dependentLocality)
    assertThat(cprAddressAfterUpdate.comment).isEqualTo(cprAddressBeforeUpdate.comment)
    assertThat(cprAddressAfterUpdate.usages.size).isEqualTo(0)
    assertThat(cprAddressAfterUpdate.contacts.size).isEqualTo(0)
  }

  @Test
  fun `consuming address updated event - cpr person does not exist - pushes message to dead letter queue`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode(), deliusAddressId = probationAddress.deliusAddressId))),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(randomCrn(), probationAddress.deliusAddressId, OFFENDER_ADDRESS_UPDATED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()

    assertThat(cprAddressAfterUpdate.id).isEqualTo(cprAddressBeforeUpdate.id)
    assertThat(cprAddressAfterUpdate.updateId).isEqualTo(cprAddressBeforeUpdate.updateId)
    assertThat(cprAddressAfterUpdate.noFixedAbode).isEqualTo(cprAddressBeforeUpdate.noFixedAbode)
    assertThat(cprAddressAfterUpdate.startDate).isEqualTo(cprAddressBeforeUpdate.startDate)
    assertThat(cprAddressAfterUpdate.endDate).isEqualTo(cprAddressBeforeUpdate.endDate)
    assertThat(cprAddressAfterUpdate.postcode).isEqualTo(cprAddressBeforeUpdate.postcode)
    assertThat(cprAddressAfterUpdate.fullAddress).isEqualTo(cprAddressBeforeUpdate.fullAddress)
    assertThat(cprAddressAfterUpdate.buildingName).isEqualTo(cprAddressBeforeUpdate.buildingName)
    assertThat(cprAddressAfterUpdate.postTown).isEqualTo(cprAddressBeforeUpdate.postTown)
    assertThat(cprAddressAfterUpdate.county).isEqualTo(cprAddressBeforeUpdate.county)
    assertThat(cprAddressAfterUpdate.uprn).isEqualTo(cprAddressBeforeUpdate.uprn)
    assertThat(cprAddressAfterUpdate.deliusAddressId).isEqualTo(cprAddressBeforeUpdate.deliusAddressId)
    assertThat(cprAddressAfterUpdate.isVerified).isEqualTo(cprAddressBeforeUpdate.isVerified)
    assertThat(cprAddressAfterUpdate.statusCode).isEqualTo(cprAddressBeforeUpdate.statusCode)
    assertThat(cprAddressAfterUpdate.buildingNumber).isEqualTo(cprAddressBeforeUpdate.buildingNumber)
    assertThat(cprAddressAfterUpdate.thoroughfareName).isEqualTo(cprAddressBeforeUpdate.thoroughfareName)
    assertThat(cprAddressAfterUpdate.dependentLocality).isEqualTo(cprAddressBeforeUpdate.dependentLocality)
    assertThat(cprAddressAfterUpdate.comment).isEqualTo(cprAddressBeforeUpdate.comment)
    assertThat(cprAddressAfterUpdate.usages.size).isEqualTo(0)
    assertThat(cprAddressAfterUpdate.contacts.size).isEqualTo(0)
  }

  @Test
  fun `consuming address updated event - cpr address does not exist - pushes message to dead letter queue`() {
    val probationAddress = randomProbationAddress()
    val personEntity = createPersonWithNewKey(
      createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode(), deliusAddressId = randomDigit().toLong()))),
    )
    val cprAddressBeforeUpdate = personEntity.addresses.first()

    stubGetRequestToProbation(probationAddress)

    publishProbationAddressEvent(personEntity.crn, probationAddress.deliusAddressId, OFFENDER_ADDRESS_UPDATED)

    expectNoMessagesOn(probationEventsQueue)
    expectOneMessageOnDlq(probationEventsQueue)

    val actualPersonEntity = awaitNotNull { personRepository.findByCrn(personEntity.crn!!) }
    assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
    val cprAddressAfterUpdate = actualPersonEntity.addresses.first()

    assertThat(cprAddressAfterUpdate.id).isEqualTo(cprAddressBeforeUpdate.id)
    assertThat(cprAddressAfterUpdate.updateId).isEqualTo(cprAddressBeforeUpdate.updateId)
    assertThat(cprAddressAfterUpdate.noFixedAbode).isEqualTo(cprAddressBeforeUpdate.noFixedAbode)
    assertThat(cprAddressAfterUpdate.startDate).isEqualTo(cprAddressBeforeUpdate.startDate)
    assertThat(cprAddressAfterUpdate.endDate).isEqualTo(cprAddressBeforeUpdate.endDate)
    assertThat(cprAddressAfterUpdate.postcode).isEqualTo(cprAddressBeforeUpdate.postcode)
    assertThat(cprAddressAfterUpdate.fullAddress).isEqualTo(cprAddressBeforeUpdate.fullAddress)
    assertThat(cprAddressAfterUpdate.buildingName).isEqualTo(cprAddressBeforeUpdate.buildingName)
    assertThat(cprAddressAfterUpdate.postTown).isEqualTo(cprAddressBeforeUpdate.postTown)
    assertThat(cprAddressAfterUpdate.county).isEqualTo(cprAddressBeforeUpdate.county)
    assertThat(cprAddressAfterUpdate.uprn).isEqualTo(cprAddressBeforeUpdate.uprn)
    assertThat(cprAddressAfterUpdate.deliusAddressId).isEqualTo(cprAddressBeforeUpdate.deliusAddressId)
    assertThat(cprAddressAfterUpdate.isVerified).isEqualTo(cprAddressBeforeUpdate.isVerified)
    assertThat(cprAddressAfterUpdate.statusCode).isEqualTo(cprAddressBeforeUpdate.statusCode)
    assertThat(cprAddressAfterUpdate.buildingNumber).isEqualTo(cprAddressBeforeUpdate.buildingNumber)
    assertThat(cprAddressAfterUpdate.thoroughfareName).isEqualTo(cprAddressBeforeUpdate.thoroughfareName)
    assertThat(cprAddressAfterUpdate.dependentLocality).isEqualTo(cprAddressBeforeUpdate.dependentLocality)
    assertThat(cprAddressAfterUpdate.comment).isEqualTo(cprAddressBeforeUpdate.comment)
    assertThat(cprAddressAfterUpdate.usages.size).isEqualTo(0)
    assertThat(cprAddressAfterUpdate.contacts.size).isEqualTo(0)
  }
}
