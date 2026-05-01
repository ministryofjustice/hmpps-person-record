package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasGetAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
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

      val sasAddressId = UUID.randomUUID()
      val sasCallbackResponse = createSasAddressGetResponse(existingPersonEntity.crn, existingAddressEntity.updateId)

      stubGetRequestToSas(sasAddressId, sasCallbackResponse)
        .also { stubPersonMatchScores() }

      publishSasAddressUpdateEvent(existingPersonEntity.crn!!, sasAddressId)

      assertAddressUpdated(existingPersonEntity.crn, sasCallbackResponse)
      assertPublishedDomainEvent(existingPersonEntity.crn)
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

      val sasAddressId = UUID.randomUUID()
      stubGetRequestToSas(sasAddressId, status = 404)

      publishSasAddressUpdateEvent(existingPersonEntity.crn!!, sasAddressId)

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

      val sasAddressId = UUID.randomUUID()
      val nonExistingAddressUpdateId = UUID.randomUUID()
      val sasCallbackResponse = createSasAddressGetResponse(existingPersonEntity.crn, nonExistingAddressUpdateId)

      stubGetRequestToSas(sasAddressId, sasCallbackResponse)

      publishSasAddressUpdateEvent(existingPersonEntity.crn!!, sasAddressId)

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

      val sasAddressId = UUID.randomUUID()
      stubGetRequestToSas(sasAddressId, sasCallbackResponse)

      publishSasAddressUpdateEvent(nonExistingPersonCrn, sasAddressId)

      expectNoMessagesOn(sasEventsQueue)
      expectOneMessageOnDlq(sasEventsQueue)
      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
    }
  }

  private fun createSasAddressGetResponse(crn: String?, cprAddressUpdateId: UUID?) = SasGetAddressResponse(
    crn = crn!!,
    cprAddressId = cprAddressUpdateId.toString(),
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

  private fun publishSasAddressUpdateEvent(crn: String, sasAddressId: UUID) {
    publishDomainEvent(
      SAS_ADDRESS_UPDATED,
      DomainEvent(
        eventType = SAS_ADDRESS_UPDATED,
        detailUrl = "/proposed-accommodations/$sasAddressId",
        additionalInformation = AdditionalInformation(
          sourceCrn = crn,
        ),
      ),
    )
  }

  private fun stubGetRequestToSas(
    sasAddressId: UUID?,
    sasCallbackResponse: SasGetAddressResponse? = null,
    status: Int = 200,
  ) {
    stubGetRequest(
      url = "/proposed-accommodations/$sasAddressId",
      body = jsonMapper.writeValueAsString(sasCallbackResponse),
      status = status,
    )
  }

  private fun assertAddressUpdated(crn: String?, expected: SasGetAddressResponse) {
    awaitAssert {
      val actualPersonEntity = personRepository.findByCrn(crn!!)!!
      assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
      val actualAddressEntity = actualPersonEntity.addresses.first()
      assertThat(actualAddressEntity.startDate).isEqualTo(expected.startDate)
      assertThat(actualAddressEntity.endDate).isEqualTo(expected.endDate)
      assertThat(actualAddressEntity.postcode).isEqualTo(expected.address.postcode)
      assertThat(actualAddressEntity.subBuildingName).isEqualTo(expected.address.subBuildingName)
      assertThat(actualAddressEntity.buildingName).isEqualTo(expected.address.buildingName)
      assertThat(actualAddressEntity.buildingNumber).isEqualTo(expected.address.buildingNumber)
      assertThat(actualAddressEntity.thoroughfareName).isEqualTo(expected.address.thoroughfareName)
      assertThat(actualAddressEntity.dependentLocality).isEqualTo(expected.address.dependentLocality)
      assertThat(actualAddressEntity.postTown).isEqualTo(expected.address.postTown)
      assertThat(actualAddressEntity.county).isEqualTo(expected.address.county)
      assertThat(actualAddressEntity.countryCode!!.name).isEqualTo(expected.address.country)
      assertThat(actualAddressEntity.uprn).isEqualTo(expected.address.uprn)
    }
  }

  private fun assertPublishedDomainEvent(crn: String?) {
    val cprAddressUpdateId = personRepository.findByCrn(crn!!)!!.addresses.first().updateId!!.toString()
    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val actualDomainEvent = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build())!!.get()
    val sqsMessage = actualDomainEvent.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    val domainEvent = jsonMapper.readValue<DomainEvent>(sqsMessage.message)

    assertThat(domainEvent.eventType).isEqualTo(CPR_ADDRESS_UPDATED)
    assertThat(domainEvent.personReference!!.identifiers!!.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers.first().type).isEqualTo("CRN")
    assertThat(domainEvent.personReference.identifiers.first().value).isEqualTo(crn)
    assertThat(domainEvent.additionalInformation?.cprAddressId).isEqualTo(cprAddressUpdateId)
    assertThat(domainEvent.version).isEqualTo(1)
    assertThat(domainEvent.description).isEqualTo("Address was updated in Core Person Record")
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn/address/$cprAddressUpdateId")
    assertThat(domainEvent.occurredAt).isNotBlank
  }
}
