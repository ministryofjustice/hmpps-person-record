package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasAddressData
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasAddressType
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasGetAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDate
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationEventListenerTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDeliusAddressId
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.Address as SasAddress

class SasAddressUpdatedEventListenerIntTest : ProbationEventListenerTestBase() {

  @Nested
  inner class Successful {

    @Test
    fun `consumes sas update event - updates address`() {
      val deliusAddressId = randomDeliusAddressId()
      val existingPersonEntity = createPerson(createRandomProbationPersonDetails().copy(addresses = listOf(Address(postcode = randomPostcode()))))
      val crn = existingPersonEntity.crn
      val existingAddressEntity = existingPersonEntity.addresses.first()
      createPersonKey().addPerson(existingPersonEntity)

      assertThat(existingAddressEntity.deliusAddressId).isNull()

      publishProbationAddressCreatedEvent(
        crn = randomCrn(),
        cprAddressId = existingAddressEntity.updateId.toString(),
        deliusAddressId = deliusAddressId,
        eventSource = DomainEventSource.CPR,
      )

      awaitNotNull {
        addressRepository.findByDeliusAddressId(deliusAddressId)
      }

      val sasCallbackResponse = createSasAddressGetResponse(crn, existingAddressEntity.updateId)

      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubGetRequestToSas(sasCallbackResponse)

      publishSasAddressUpdateEvent()

      val actualAddress = assertAddressUpdated(crn, sasCallbackResponse, deliusAddressId)
      assertDomainEventPublishedAfterSasEvent(
        expectedEventType = CPR_PROBATION_ADDRESS_UPDATED,
        crn = crn!!,
        cprAddressUpdateId = actualAddress.updateId.toString(),
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

      publishSasAddressUpdateEvent()

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

      publishSasAddressUpdateEvent()

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

      publishSasAddressUpdateEvent()

      expectNoMessagesOn(sasEventsQueue)
      expectOneMessageOnDlq(sasEventsQueue)
      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
    }
  }

  private fun createSasAddressGetResponse(crn: String?, cprAddressUpdateId: UUID?) = SasGetAddressResponse(
    data = SasAddressData(
      crn = crn!!,
      cprAddressId = cprAddressUpdateId.toString(),
      startDate = LocalDate.now().minusYears(10),
      endDate = LocalDate.now().plusYears(10),
      noFixedAbode = randomBoolean(),
      typeVerified = randomBoolean(),
      address = SasAddress(
        postcode = randomPostcode(),
        subBuildingName = randomName(),
        buildingName = randomName(),
        buildingNumber = randomBuildingNumber(),
        thoroughfareName = randomName(),
        dependentLocality = randomName(),
        postTown = randomPostcode(),
        county = randomName(),
        countryCode = "E",
        uprn = randomUprn(),
      ),
      statusCode = SasAddressStatus(
        code = randomAddressStatusCode().name,
        description = randomLowerCaseString(),
      ),
      usage = SasAddressType(
        code = randomAddressUsageCode().name,
        description = randomLowerCaseString(),
      ),
    ),
  )

  private fun publishSasAddressUpdateEvent() {
    publishDomainEvent(
      SasAddressUpdated(
        eventType = SAS_ADDRESS_UPDATED,
        occurredAt = Instant.now().asStringWithUkZone(),
        detailUrl = "/accommodations/1234",
      ),
    )
  }

  private fun stubGetRequestToSas(
    sasCallbackResponse: SasGetAddressResponse? = null,
    status: Int = 200,
  ) {
    stubGetRequest(
      url = "/accommodations/1234",
      body = jsonMapper.writeValueAsString(sasCallbackResponse),
      status = status,
    )
  }

  private fun assertAddressUpdated(crn: String?, expected: SasGetAddressResponse, existingDeliusAddressId: Long): AddressEntity {
    var actualAddressEntity: AddressEntity? = null
    awaitAssert {
      val expectedSasAddress = expected.data
      val actualPersonEntity = personRepository.findByCrn(crn!!)!!
      assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
      actualAddressEntity = actualPersonEntity.addresses.first()

      assertThat(actualAddressEntity.postcode).isEqualTo(expectedSasAddress.address.postcode)
      assertThat(actualAddressEntity.deliusAddressId).isEqualTo(existingDeliusAddressId)
      assertThat(actualAddressEntity.startDate!!.toUkLocalDate()).isEqualTo(expectedSasAddress.startDate)
      assertThat(actualAddressEntity.endDate!!.toUkLocalDate()).isEqualTo(expectedSasAddress.endDate)
      assertThat(actualAddressEntity.noFixedAbode).isEqualTo(expectedSasAddress.noFixedAbode)
      assertThat(actualAddressEntity.isVerified).isEqualTo(expectedSasAddress.typeVerified)
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
    return actualAddressEntity!!
  }
}
