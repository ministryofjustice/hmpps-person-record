package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.OK
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalCountry
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
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
import java.util.UUID.randomUUID

class ProbationAddressAPIControllerTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {
    @Test
    fun `should return canonical address record when record exists`() {
      val crn = randomCrn()
      val person = createPersonWithNewKey(
        createRandomProbationPersonDetails(crn).copy(
          addresses = listOf(
            Address(
              noFixedAbode = randomBoolean(), startDate = randomDate(), endDate = randomDate(), postcode = randomPostcode(), buildingName = randomName(),
              subBuildingName = randomName(), buildingNumber = randomBuildingNumber(), thoroughfareName = randomName(), dependentLocality = randomName(),
              postTown = randomName(), county = randomName(), countryCode = randomCountryCode(), uprn = randomUprn(), statusCode = randomAddressStatusCode(),
              comment = randomName(), usages = listOf(AddressUsage(randomAddressUsageCode(), randomBoolean())),
            ),
          ),
        ),
      )
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
      val otherPerson = createPersonWithNewKey(createRandomProbationPersonDetails())
      val otherAddressId = otherPerson.addresses.first().updateId.toString()
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

  private fun assertCanonicalAddress(expectedAddress: AddressEntity, actualAddress: CanonicalAddress) {
    val canonicalAddress = CanonicalAddress(
      cprAddressId = expectedAddress.updateId!!.toString(),
      noFixedAbode = expectedAddress.noFixedAbode,
      startDate = expectedAddress.startDate?.toString(),
      endDate = expectedAddress.endDate?.toString(),
      postcode = expectedAddress.postcode,
      subBuildingName = expectedAddress.subBuildingName,
      buildingName = expectedAddress.buildingName,
      buildingNumber = expectedAddress.buildingNumber,
      thoroughfareName = expectedAddress.thoroughfareName,
      dependentLocality = expectedAddress.dependentLocality,
      postTown = expectedAddress.postTown,
      county = expectedAddress.county,
      country = CanonicalCountry.from(expectedAddress.countryCode),
      uprn = expectedAddress.uprn,
      status = CanonicalAddressStatus.from(expectedAddress.statusCode),
      comment = expectedAddress.comment,
      usages = expectedAddress.usages.map {
        CanonicalAddressUsage(
          CanonicalAddressUsageCode.from(it.usageCode),
          it.active,
        )
      },
    )

    assertThat(actualAddress)
      .usingRecursiveComparison()
      .isEqualTo(canonicalAddress)
  }
}
