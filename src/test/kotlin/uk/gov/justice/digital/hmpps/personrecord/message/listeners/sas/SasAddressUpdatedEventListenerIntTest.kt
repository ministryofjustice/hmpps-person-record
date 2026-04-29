package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasGetAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.Address as SasAddress

class SasAddressUpdatedEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Nested
  inner class Successful {

    @Test
    fun `updates address - publishes domain event`() {
      val existingPersonEntity = createPerson(createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode()))))
      val existingAddressEntity = existingPersonEntity.addresses.first()
      createPersonKey()
        .addPerson(existingPersonEntity)

      val sasCallbackResponse = createSasAddressGetResponse(existingPersonEntity.crn, existingAddressEntity.updateId)

      val sasAddressId = randomDigit() // this will be an uuid
      stubGetRequestToSas(sasAddressId, sasCallbackResponse)
        .also { stubPersonMatchScores() }

      publishSasAddressUpdateEvent(existingPersonEntity.crn!!, sasAddressId)

      awaitAssert {
        val actualPersonEntity = personRepository.findByCrn(existingPersonEntity.crn!!)!!
        assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
        val actualAddressEntity = actualPersonEntity.addresses.first()
        assertThat(actualAddressEntity.startDate).isEqualTo(sasCallbackResponse.startDate)
        assertThat(actualAddressEntity.endDate).isEqualTo(sasCallbackResponse.endDate)
        assertThat(actualAddressEntity.postcode).isEqualTo(sasCallbackResponse.address.postcode)
        assertThat(actualAddressEntity.subBuildingName).isEqualTo(sasCallbackResponse.address.subBuildingName)
        assertThat(actualAddressEntity.buildingName).isEqualTo(sasCallbackResponse.address.buildingName)
        assertThat(actualAddressEntity.buildingNumber).isEqualTo(sasCallbackResponse.address.buildingNumber)
        assertThat(actualAddressEntity.thoroughfareName).isEqualTo(sasCallbackResponse.address.thoroughfareName)
        assertThat(actualAddressEntity.dependentLocality).isEqualTo(sasCallbackResponse.address.dependentLocality)
        assertThat(actualAddressEntity.postTown).isEqualTo(sasCallbackResponse.address.postTown)
        assertThat(actualAddressEntity.county).isEqualTo(sasCallbackResponse.address.county)
        assertThat(actualAddressEntity.countryCode!!.name).isEqualTo(sasCallbackResponse.address.country)
        assertThat(actualAddressEntity.uprn).isEqualTo(sasCallbackResponse.address.uprn)
      }

      // assert domain event published
    }
  }

  @Nested
  inner class FailureScenarios {
    fun `address not returned from sas - pushes event to dead letter queue`() {
    }

    fun `cpr address does not exist - pushed to dead letter queue`() {
    }

    fun `cpr person does not exist - pushes to dead letter queue`() {
    }
  }

  private fun createSasAddressGetResponse(crn: String?, updateId: UUID?) = SasGetAddressResponse(
    crn = crn!!,
    cprAddressUpdateId = updateId.toString(),
    startDate = LocalDate.now().minusYears(10),
    endDate = LocalDate.now().plusYears(10),
    address = SasAddress(
      postcode = randomPostcode(),
      subBuildingName = randomName(),
      buildingName = randomName(),
      buildingNumber = randomBuildingNumber(),
      thoroughfareName = randomName(),
      dependentLocality = randomName(),
      postTown = randomPostcode(),
      county = randomName(),
      country = randomCountryCode().name,
      uprn = randomUprn(),
    ),
  )

  private fun publishSasAddressUpdateEvent(crn: String, sasAddressId: String) {
    publishDomainEvent(
      SAS_ADDRESS_UPDATED,
      DomainEvent(
        eventType = SAS_ADDRESS_UPDATED,
        additionalInformation = AdditionalInformation(
          sourceCrn = crn,
          sasAddressId = sasAddressId,
        ),
      ),
    )
  }

  private fun stubGetRequestToSas(
    sasAddressId: String,
    sasCallbackResponse: SasGetAddressResponse,
  ) {
    stubGetRequest(
      url = "/proposed-accommodations/$sasAddressId",
      body = jsonMapper.writeValueAsString(sasCallbackResponse),
      status = 200,
    )
  }
}
