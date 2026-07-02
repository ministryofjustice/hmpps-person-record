package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.OK
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.test.assertCanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomContactType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import uk.gov.justice.digital.hmpps.personrecord.test.randomZonedDateTime
import java.util.UUID.randomUUID

class ProbationAddressGetAPIControllerTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {
    @Test
    fun `should return canonical address record when record exists`() {
      val crn = randomCrn()
      val person = createPersonWithNewKey(
        createRandomProbationPersonDetails(crn),
        configure = addAddressToRecord(Address(
            noFixedAbode = randomBoolean(), startDate = randomZonedDateTime(), endDate = randomZonedDateTime(), postcode = randomPostcode(), buildingName = randomName(),
            subBuildingName = randomName(), buildingNumber = randomBuildingNumber(), thoroughfareName = randomName(), dependentLocality = randomName(),
            postTown = randomName(), county = randomName(), countryCode = randomCountryCode(), uprn = randomUprn(), statusCode = randomAddressStatusCode(),
            comment = randomName(), isVerified = randomBoolean(), usages = listOf(AddressUsage(randomAddressUsageCode(), randomBoolean())),
            contacts = listOf(Contact(randomContactType(), randomPhoneNumber(), "+44")))
      ))

      val expectedAddress = person.addresses.first()

      val responseBody = sendGetRequestAsserted<CanonicalAddress>(
        url = probationAddressApiUrl(crn, expectedAddress.updateId.toString()),
        roles = listOf(API_READ_ONLY),
        expectedStatus = OK,
      ).returnResult().responseBody!!

      assertCanonicalAddress(expectedAddress, responseBody)
    }
  }

  @Nested
  inner class ErrorScenarios {
    @Test
    fun `should return 404 not found when probation record does not exist`() {
      val crn = randomCrn()
      val otherAddressId = randomUUID().toString()
      val expectedErrorMessage = "Not found: $otherAddressId"
      webTestClient.get()
        .uri(probationAddressApiUrl(crn, otherAddressId))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return 404 not found when address record does not exist`() {
      val crn = randomCrn()
      val addressId = randomUUID()
      createPersonWithNewKey(createRandomProbationPersonDetails(crn).copy(addresses = listOf()))

      val expectedErrorMessage = "Not found: $addressId"
      webTestClient.get()
        .uri(probationAddressApiUrl(crn, addressId.toString()))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.get()
        .uri(probationAddressApiUrl("accessdenied", "accessDenied"))
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      webTestClient.get()
        .uri(probationAddressApiUrl("unauthorised", "unauthorised"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun probationAddressApiUrl(crn: String, cprAddressId: String) = "/person/probation/$crn/address/$cprAddressId"
}
