package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.node.ObjectNode
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddress
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

    @Test
    fun `should create a new address and recluster - with invalid country code`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val crn = randomCrn()
      val newAddress = createRandomProbationAddress()
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      val responseBody = sendPostRequestAsserted<ProbationCreateAddressResponse>(
        url = probationAddressApiUrl(crn),
        body = jsonMapper.writeValueAsString(newAddress).replace("\"typeVerified\"", "\"countryCode\": \"INVALID\", \"typeVerified\""),
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

  @Nested
  @ActiveProfiles("preprod")
  inner class FeatureFlagPreprod {
    @Test
    fun `endpoint not available in preprod`() {
      val crn = randomCrn()
      val newAddress = createRandomProbationAddress()
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      sendPostRequestAsserted<Unit>(
        url = probationAddressApiUrl(crn),
        body = newAddress,
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )

      awaitAssert {
        val personEntity = personRepository.findByCrn(crn) ?: fail("No person found with id $crn")
        assertThat(personEntity.addresses).isEmpty()
      }
    }
  }

  @Nested
  @ActiveProfiles("prod")
  inner class FeatureFlagProd {
    @Test
    fun `endpoint not available in prod`() {
      val crn = randomCrn()
      val newAddress = createRandomProbationAddress()
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = emptyList()))

      sendPostRequestAsserted<Unit>(
        url = probationAddressApiUrl(crn),
        body = newAddress,
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )

      awaitAssert {
        val personEntity = personRepository.findByCrn(crn) ?: fail("No person found with id $crn")
        assertThat(personEntity.addresses).isEmpty()
      }
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
