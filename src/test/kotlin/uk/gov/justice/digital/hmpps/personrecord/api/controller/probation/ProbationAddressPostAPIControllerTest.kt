package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.Address
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn

class ProbationAddressPostAPIControllerTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {
    @Test
    fun `should create a new address and recluster`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val crn = randomCrn()
      val newAddress = createRandomAddress()
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
      val newAddress = createRandomAddress()
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
  }

  @Nested
  inner class ErrorScenarios {
    @Test
    fun `should return 404 not found when probation record does not exist`() {
      sendPostRequestAsserted<Unit>(
        url = probationAddressApiUrl(randomCrn()),
        body = createRandomAddress(),
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )
    }

    @Test
    fun `should return 404 not found when probation record is a merged record`() {
      val mergedCrn = randomCrn()
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val mergedPerson = createPerson(createRandomProbationPersonDetails(mergedCrn)) { mergedTo = person.id }
      sendPostRequestAsserted<Unit>(
        url = probationAddressApiUrl(mergedCrn),
        body = createRandomAddress(),
        roles = listOf(PROBATION_API_READ_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPostRequestAsserted<Unit>(
        url = probationAddressApiUrl(randomCrn()),
        body = createRandomAddress(),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendPostRequestAsserted<Unit>(
        url = probationAddressApiUrl(randomCrn()),
        body = createRandomAddress(),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }
  }

  private fun probationAddressApiUrl(crn: String) = "/person/probation/$crn/address"

  private fun createRandomAddress(): Address = Address(
    noFixedAbode = false,
    startDate = randomDate(),
    endDate = randomDate(),
    postcode = randomPostcode(),
    uprn = randomUprn(),
    subBuildingName = randomName(),
    buildingName = randomName(),
    buildingNumber = randomBuildingNumber(),
    thoroughfareName = randomName(),
    dependentLocality = randomName(),
    postTown = randomName(),
    county = randomName(),
    countryCode = randomCountryCode(),
    comment = randomName(),
    statusCode = randomAddressStatusCode(),
    usages = listOf(AddressUsage(randomAddressUsageCode(), randomBoolean())),
  )

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
    assertThat(actualAddress.usages.size).isEqualTo(expectedAddress.usages.size)
    expectedAddress.usages.zip(actualAddress.usages).forEach { (e, a) ->
      assertThat(a.usageCode).isEqualTo(e.addressUsageCode)
      assertThat(a.active).isEqualTo(e.isActive)
    }
  }
}
