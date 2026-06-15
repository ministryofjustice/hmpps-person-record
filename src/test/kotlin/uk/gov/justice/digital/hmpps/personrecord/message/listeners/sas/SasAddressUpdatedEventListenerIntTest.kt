package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasGetAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDate
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationEventListenerTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.util.UUID

class SasAddressUpdatedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Nested
  inner class Successful {

    @Test
    fun `consumes sas update event - updates address`() {
      val existingPersonEntity = createPerson(createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode()))))
      val crn = existingPersonEntity.crn
      val existingAddressEntity = existingPersonEntity.addresses.first()
      createPersonKey()
        .addPerson(existingPersonEntity)

      val sasCallbackResponse = createSasAddressGetResponse(crn, existingAddressEntity.updateId)

      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubGetRequestToSas(sasCallbackResponse)

      publishSasAddressUpdateEvent(crn!!)

      assertAddressUpdated(crn, sasCallbackResponse)
      assertDomainEventPublishedAfterSasEvent(
        expectedEventType = CPR_PROBATION_ADDRESS_UPDATED,
        crn = crn,
      )
    }
  }

  @Nested
  inner class FailureScenarios {

    @Test
    fun `address not returned from sas - pushes event to dead letter queue`() {
      val existingPersonEntity = createPerson(createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode()))))
      val existingAddressEntity = existingPersonEntity.addresses.first()
      createPersonKey()
        .addPerson(existingPersonEntity)

      stubGetRequestToSas(status = 404)

      publishSasAddressUpdateEvent(existingPersonEntity.crn!!)

      expectNoMessagesOn(sasEventsQueue)
      expectOneMessageOnDlq(sasEventsQueue)
      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
      val actualAddress = personRepository.findByCrn(existingPersonEntity.crn!!)!!.addresses.first()
      assertThat(actualAddress.postcode).isEqualTo(existingAddressEntity.postcode)
    }

    @Test
    fun `cpr address does not exist - pushed to dead letter queue`() {
      val existingPersonEntity = createPerson(createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode()))))
      createPersonKey()
        .addPerson(existingPersonEntity)

      val nonExistingAddressUpdateId = UUID.randomUUID()
      val sasCallbackResponse = createSasAddressGetResponse(existingPersonEntity.crn, nonExistingAddressUpdateId)

      stubGetRequestToSas(sasCallbackResponse)

      publishSasAddressUpdateEvent(existingPersonEntity.crn!!)

      expectNoMessagesOn(sasEventsQueue)
      expectOneMessageOnDlq(sasEventsQueue)
      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
    }

    @Test
    fun `cpr person does not exist - pushes to dead letter queue`() {
      val existingPersonEntity = createPerson(createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode()))))
      val existingAddressEntity = existingPersonEntity.addresses.first()
      createPersonKey()
        .addPerson(existingPersonEntity)

      val nonExistingPersonCrn = randomCrn()
      val sasCallbackResponse = createSasAddressGetResponse(nonExistingPersonCrn, existingAddressEntity.updateId)

      stubGetRequestToSas(sasCallbackResponse)

      publishSasAddressUpdateEvent(nonExistingPersonCrn)

      expectNoMessagesOn(sasEventsQueue)
      expectOneMessageOnDlq(sasEventsQueue)
      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
    }
  }

  private fun publishSasAddressUpdateEvent(crn: String) {
    publishDomainEvent(
      SAS_ADDRESS_UPDATED,
      DomainEvent(
        eventType = SAS_ADDRESS_UPDATED,
        detailUrl = "/accommodations/1234",
        additionalInformation = AdditionalInformation(
          sourceCrn = crn,
        ),
      ),
    )
  }

  private fun assertAddressUpdated(crn: String?, expected: SasGetAddressResponse) {
    awaitAssert {
      val expectedSasAddress = expected.data
      val actualPersonEntity = personRepository.findByCrn(crn!!)!!
      assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
      val actualAddressEntity = actualPersonEntity.addresses.first()
      assertThat(actualAddressEntity.startDate!!.toUkLocalDate()).isEqualTo(expectedSasAddress.startDate)
      assertThat(actualAddressEntity.endDate!!.toUkLocalDate()).isEqualTo(expectedSasAddress.endDate)
      assertThat(actualAddressEntity.noFixedAbode).isEqualTo(expectedSasAddress.noFixedAbode)
      assertThat(actualAddressEntity.isVerified).isEqualTo(expectedSasAddress.typeVerified)
      assertThat(actualAddressEntity.postcode).isEqualTo(expectedSasAddress.address.postcode)
      assertThat(actualAddressEntity.subBuildingName).isEqualTo(expectedSasAddress.address.subBuildingName)
      assertThat(actualAddressEntity.buildingName).isEqualTo(expectedSasAddress.address.buildingName)
      assertThat(actualAddressEntity.buildingNumber).isEqualTo(expectedSasAddress.address.buildingNumber)
      assertThat(actualAddressEntity.thoroughfareName).isEqualTo(expectedSasAddress.address.thoroughfareName)
      assertThat(actualAddressEntity.dependentLocality).isEqualTo(expectedSasAddress.address.dependentLocality)
      assertThat(actualAddressEntity.postTown).isEqualTo(expectedSasAddress.address.postTown)
      assertThat(actualAddressEntity.county).isEqualTo(expectedSasAddress.address.county)
      assertThat(actualAddressEntity.uprn).isEqualTo(expectedSasAddress.address.uprn)
      assertThat(actualAddressEntity.statusCode!!.name).isEqualTo(expectedSasAddress.statusCode!!.code)
      val actualAddressUsages = actualAddressEntity.usages
      assertThat(actualAddressUsages.size).isEqualTo(1)
      assertThat(actualAddressUsages.first().usageCode.name).isEqualTo(expectedSasAddress.usage!!.code)
      assertThat(actualAddressUsages.first().active).isEqualTo(true)
    }
  }
}
