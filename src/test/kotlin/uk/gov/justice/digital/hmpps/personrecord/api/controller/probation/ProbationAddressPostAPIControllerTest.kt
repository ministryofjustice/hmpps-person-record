package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import tools.jackson.databind.node.ObjectNode
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.Address
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class ProbationAddressPostAPIControllerTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {
    @Test
    fun `should create a new address and recluster`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val crn = randomCrn()
      val newAddress = createRandomProbationAddress()
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      val responseBody = sendPostRequestAsserted<ProbationCreateAddressResponse>(
        url = probationAddressApiUrl(crn),
        body = newAddress,
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = HttpStatus.CREATED,
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
    fun `should create new address and not recluster passive record`() {
      val crn = randomCrn()
      val newAddress = createRandomProbationAddress()
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList())) { this.passiveState = true }

      val responseBody = sendPostRequestAsserted<ProbationCreateAddressResponse>(
        url = probationAddressApiUrl(crn),
        body = newAddress,
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = HttpStatus.CREATED,
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
      stubPersonMatchUpsert()
      stubPersonMatchScores()

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
        expectedStatus = HttpStatus.CREATED,
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
    fun `should return 404 not found when probation record does not exist`() {
      sendPostRequestAsserted<Unit>(
        url = probationAddressApiUrl(randomCrn()),
        body = createRandomProbationAddress(),
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )
    }

    @Test
    fun `should return 404 not found when probation record is a merged record`() {
      val mergedCrn = randomCrn()
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      createPerson(createRandomProbationPersonDetails(mergedCrn)) { mergedTo = person.id }
      sendPostRequestAsserted<Unit>(
        url = probationAddressApiUrl(mergedCrn),
        body = createRandomProbationAddress(),
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
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

  private fun assertAddressValues(expectedAddress: Address, actualAddress: AddressEntity) {
    assertThat(actualAddress.updateId.toString()).isNotEmpty()
    assertThat(actualAddress.noFixedAbode).isEqualTo(expectedAddress.noFixedAbode)
    assertThat(actualAddress.startDate).isEqualTo(expectedAddress.startDate)
    assertThat(actualAddress.endDate).isEqualTo(expectedAddress.endDate)
    assertThat(actualAddress.postcode).isEqualTo(expectedAddress.postcode)
    assertThat(actualAddress.uprn).isEqualTo(expectedAddress.uprn)
    assertThat(actualAddress.subBuildingName).isEqualTo(expectedAddress.subBuildingName)
    assertThat(actualAddress.buildingName).isEqualTo(expectedAddress.buildingName)
    assertThat(actualAddress.buildingNumber).isEqualTo(expectedAddress.buildingNumber)
    assertThat(actualAddress.thoroughfareName).isEqualTo(expectedAddress.thoroughfareName)
    assertThat(actualAddress.dependentLocality).isEqualTo(expectedAddress.dependentLocality)
    assertThat(actualAddress.postTown).isEqualTo(expectedAddress.postTown)
    assertThat(actualAddress.county).isEqualTo(expectedAddress.county)
    assertThat(actualAddress.countryCode).isEqualTo(expectedAddress.countryCode)
    assertThat(actualAddress.comment).isEqualTo(expectedAddress.comment)
    assertThat(actualAddress.statusCode).isEqualTo(expectedAddress.statusCode)
    assertThat(actualAddress.isVerified).isEqualTo(expectedAddress.typeVerified)
    assertThat(actualAddress.usages.size).isEqualTo(expectedAddress.usages.size)
    expectedAddress.usages.zip(actualAddress.usages).forEach { (expected, actual) ->
      assertThat(actual.usageCode).isEqualTo(expected.usageCode)
      assertThat(actual.active).isEqualTo(expected.isActive)
    }
    expectedAddress.contacts.zip(actualAddress.contacts).forEach { (expected, actual) ->
      assertThat(actual.contactType).isEqualTo(expected.typeCode)
      assertThat(actual.contactValue).isEqualTo(expected.value)
      assertThat(actual.extension).isEqualTo(expected.extension)
    }
  }
}
