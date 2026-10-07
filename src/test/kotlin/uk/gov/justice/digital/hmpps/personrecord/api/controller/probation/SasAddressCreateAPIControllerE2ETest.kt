package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_ADDRESS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.M
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.PR
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class SasAddressCreateAPIControllerE2ETest : E2ETestBase() {

  @Nested
  inner class SuccessfulProcessing {
    @Test
    fun `should create a new proposed address and recluster`() {
      val crn = randomCrn()
      val newAddress = createRandomProbationAddress().copy(statusCode = PR)
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      val responseBody = sendPostRequestAsserted<ProbationCreateAddressResponse>(
        url = probationAddressApiUrl(crn),
        body = newAddress,
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = CREATED,
      ).returnResult().responseBody!!

      awaitAssert {
        val personEntity = personRepository.findByCrn(crn) ?: fail("No person found with id $crn")
        assertThat(personEntity.addresses.size).isEqualTo(1)

        val actualAddress = personEntity.addresses.first()
        assertAddressValues(newAddress, actualAddress)

        assertThat(responseBody.crn).isEqualTo(crn)
        assertThat(responseBody.cprAddressId).isEqualTo(actualAddress.updateId.toString())
      }
    }

    @Test
    fun `should create a new proposed address when there is an existing main address`() {
      val crn = randomCrn()
      val newAddress = createRandomProbationAddress().copy(statusCode = PR)
      val mainAddress = createRandomProbationAddress().copy(statusCode = M)
      createPersonWithNewKey(createRandomProbationPersonDetails(crn), configure = addAddressToRecord(Address.from(mainAddress)))

      val responseBody = sendPostRequestAsserted<ProbationCreateAddressResponse>(
        url = probationAddressApiUrl(crn),
        body = newAddress,
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = CREATED,
      ).returnResult().responseBody!!

      awaitAssert {
        val personEntity = personRepository.findByCrn(crn)!!
        assertThat(personEntity.addresses.size).isEqualTo(2)

        assertAddressValues(mainAddress, personEntity.getMainAddress())

        val proposedAddress = personEntity.addresses.first { it.statusCode == PR }
        assertAddressValues(newAddress, proposedAddress)

        assertThat(responseBody.crn).isEqualTo(crn)
        assertThat(responseBody.cprAddressId).isEqualTo(proposedAddress.updateId.toString())
      }
    }

    @Test
    fun `should create a new main address`() {
      val crn = randomCrn()
      val newAddress = createRandomProbationAddress().copy(statusCode = M)
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      val responseBody = sendPostRequestAsserted<ProbationCreateAddressResponse>(
        url = probationAddressApiUrl(crn),
        body = newAddress,
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = CREATED,
      ).returnResult().responseBody!!

      awaitAssert {
        val personEntity = personRepository.findByCrn(crn)!!
        assertThat(personEntity.addresses.size).isEqualTo(1)

        val actualAddress = personEntity.addresses.first()
        assertAddressValues(newAddress, actualAddress)

        assertThat(responseBody.crn).isEqualTo(crn)
        assertThat(responseBody.cprAddressId).isEqualTo(actualAddress.updateId.toString())
      }
    }

    @Test
    fun `should create a new main address when there is already a main address`() {
      val crn = randomCrn()
      val newAddress = createRandomProbationAddress().copy(statusCode = M)
      val person = createPersonWithNewKey(createRandomProbationPersonDetails(crn), configure = addAddressToRecord(Address.from(createRandomProbationAddress().copy(statusCode = M))))

      val existingAddressId = person.addresses.first().updateId!!

      val responseBody = sendPostRequestAsserted<ProbationCreateAddressResponse>(
        url = probationAddressApiUrl(crn),
        body = newAddress,
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = CREATED,
      ).returnResult().responseBody!!

      awaitAssert {
        val personEntity = personRepository.findByCrn(crn)!!
        assertThat(personEntity.addresses.size).isEqualTo(2)

        val mainAddress = personEntity.getMainAddress()
        assertAddressValues(newAddress, mainAddress)
        val previousAddress = addressRepository.findByUpdateId(existingAddressId)!!
        assertThat(previousAddress.statusCode).isEqualTo(AddressStatusCode.P)
        assertThat(previousAddress.endDate).isEqualTo(newAddress.startDate)
        assertThat(responseBody.crn).isEqualTo(crn)
        assertThat(responseBody.cprAddressId).isEqualTo(mainAddress.updateId.toString())
      }

      val queue = testOnlyCPRDomainEventsQueue!!
      await untilCallTo {
        queue.sqsClient.countMessagesOnQueue(queue.queueUrl).get()
      } matches { it == 2 }

      val updatedMessage = receiveNextMessageOnQueue(queue)
      assertThat(updatedMessage.getEventType()).isEqualTo(CPR_PROBATION_ADDRESS_UPDATED)
      val updatedEvent = jsonMapper.readValue<CprAddressUpdated>(updatedMessage.message)
      assertThat(updatedEvent.additionalInformation.cprAddressId).isEqualTo(existingAddressId)
      assertThat(updatedEvent.personReference.identifiers?.single()?.value).isEqualTo(crn)

      val createdMessage = receiveNextMessageOnQueue(queue)
      assertThat(createdMessage.getEventType()).isEqualTo(CPR_PROBATION_ADDRESS_CREATED)
      val createdEvent = jsonMapper.readValue<CprAddressCreated>(createdMessage.message)
      assertThat(createdEvent.additionalInformation.cprAddressId.toString()).isEqualTo(responseBody.cprAddressId)
      assertThat(createdEvent.personReference.identifiers?.single()?.value).isEqualTo(crn)
    }

    @Test
    fun `should create new address and not recluster passive record`() {
      val crn = randomCrn()
      val newAddress = createRandomProbationAddress()
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList())) { this.passiveState = true }

      val responseBody = sendPostRequestAsserted<ProbationCreateAddressResponse>(
        url = probationAddressApiUrl(crn),
        body = newAddress,
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = CREATED,
      ).returnResult().responseBody!!

      awaitAssert {
        val personEntity = personRepository.findByCrn(crn) ?: fail("No person found with id $crn")
        assertThat(personEntity.addresses.size).isEqualTo(1)

        val actualAddress = personEntity.addresses.first()
        assertAddressValues(newAddress, actualAddress)

        assertThat(responseBody.crn).isEqualTo(crn)
        assertThat(responseBody.cprAddressId).isEqualTo(actualAddress.updateId.toString())
      }
    }

    @Test
    fun `should create a new address with typeVerified as true when typeVerified is not supplied`() {
      val crn = randomCrn()
      val newAddress = createRandomProbationAddress()
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      // simulate missing field
      val json = jsonMapper.valueToTree<ObjectNode>(newAddress)
      json.remove("typeVerified")

      val responseBody = sendPostRequestAsserted<ProbationCreateAddressResponse>(
        url = probationAddressApiUrl(crn),
        body = json,
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = CREATED,
      ).returnResult().responseBody!!

      awaitAssert {
        val personEntity = personRepository.findByCrn(crn) ?: fail("No person found with id $crn")
        assertThat(personEntity.addresses.size).isEqualTo(1)

        val actualAddress = personEntity.addresses.first()
        assertAddressValues(newAddress, actualAddress)

        assertThat(responseBody.crn).isEqualTo(crn)
        assertThat(responseBody.cprAddressId).isEqualTo(actualAddress.updateId.toString())
      }
    }

    @Test
    fun `should create a new address and recluster - with invalid country code`() {
      val crn = randomCrn()
      val newAddress = createRandomProbationAddress()
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      val responseBody = sendPostRequestAsserted<ProbationCreateAddressResponse>(
        url = probationAddressApiUrl(crn),
        body = jsonMapper.writeValueAsString(newAddress).replace("\"typeVerified\"", "\"countryCode\": \"INVALID\", \"typeVerified\""),
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = CREATED,
      ).returnResult().responseBody!!

      awaitAssert {
        val personEntity = personRepository.findByCrn(crn) ?: fail("No person found with id $crn")
        assertThat(personEntity.addresses.size).isEqualTo(1)

        val actualAddress = personEntity.addresses.first()
        assertAddressValues(newAddress, actualAddress)

        assertThat(responseBody.crn).isEqualTo(crn)
        assertThat(responseBody.cprAddressId).isEqualTo(actualAddress.updateId.toString())
      }
    }
  }

  @Nested
  inner class ErrorScenarios {
    @Test
    fun `should return 500 not found when probation record does not exist`() {
      sendPostRequestAsserted<Unit>(
        url = probationAddressApiUrl(randomCrn()),
        body = createRandomProbationAddress(),
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = HttpStatus.INTERNAL_SERVER_ERROR,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPostRequestAsserted<Unit>(
        url = probationAddressApiUrl(randomCrn()),
        body = createRandomProbationAddress(),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendPostRequestAsserted<Unit>(
        url = probationAddressApiUrl(randomCrn()),
        body = createRandomProbationAddress(),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }
  }

  private fun probationAddressApiUrl(crn: String) = "/person/probation/$crn/address"

  private fun assertAddressValues(expectedProbationCreateAddress: ProbationCreateAddress, actualAddress: AddressEntity) {
    assertThat(actualAddress.updateId.toString()).isNotEmpty()
    assertThat(actualAddress.noFixedAbode).isEqualTo(expectedProbationCreateAddress.noFixedAbode)
    assertThat(actualAddress.startDate).isEqualTo(expectedProbationCreateAddress.startDate)
    assertThat(actualAddress.endDate).isEqualTo(expectedProbationCreateAddress.endDate)
    assertThat(actualAddress.postcode).isEqualTo(expectedProbationCreateAddress.postcode)
    assertThat(actualAddress.uprn).isEqualTo(expectedProbationCreateAddress.uprn)
    assertThat(actualAddress.subBuildingName).isEqualTo(expectedProbationCreateAddress.subBuildingName)
    assertThat(actualAddress.buildingName).isEqualTo(expectedProbationCreateAddress.buildingName)
    assertThat(actualAddress.buildingNumber).isEqualTo(expectedProbationCreateAddress.buildingNumber)
    assertThat(actualAddress.thoroughfareName).isEqualTo(expectedProbationCreateAddress.thoroughfareName)
    assertThat(actualAddress.dependentLocality).isEqualTo(expectedProbationCreateAddress.dependentLocality)
    assertThat(actualAddress.postTown).isEqualTo(expectedProbationCreateAddress.postTown)
    assertThat(actualAddress.county).isEqualTo(expectedProbationCreateAddress.county)
    assertThat(actualAddress.comment).isEqualTo(expectedProbationCreateAddress.comment)
    assertThat(actualAddress.statusCode).isEqualTo(expectedProbationCreateAddress.statusCode)
    assertThat(actualAddress.isVerified).isEqualTo(expectedProbationCreateAddress.typeVerified)
    assertThat(actualAddress.usages.size).isEqualTo(expectedProbationCreateAddress.usages.size)
    expectedProbationCreateAddress.usages.zip(actualAddress.usages).forEach { (expected, actual) ->
      assertThat(actual.usageCode).isEqualTo(expected.usageCode)
      assertThat(actual.active).isEqualTo(expected.isActive)
    }
    expectedProbationCreateAddress.contacts.zip(actualAddress.contacts).forEach { (expected, actual) ->
      assertThat(actual.contactType).isEqualTo(expected.typeCode)
      assertThat(actual.contactValue).isEqualTo(expected.value)
      assertThat(actual.extension).isEqualTo(expected.extension)
    }
  }
}

private fun PersonEntity.getMainAddress(): AddressEntity = this.addresses.first { it.statusCode == AddressStatusCode.M }
