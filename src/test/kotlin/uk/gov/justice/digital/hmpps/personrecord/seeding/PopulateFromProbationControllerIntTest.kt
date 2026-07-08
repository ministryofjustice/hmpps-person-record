package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDeliusAddressId
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import uk.gov.justice.digital.hmpps.personrecord.test.randomZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationCaseResponse

class PopulateFromProbationControllerIntTest : WebTestBase() {

  @Nested
  inner class MissingRecord {

    @Test
    fun `should not populate when record is merged`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val mergedPersonDetails = createRandomProbationCase()
      val mergedPerson = createPerson(Person.from(mergedPersonDetails)) { mergedTo = person.id }
      mergedPerson.assertMergedTo(person)

      val mergedCrn = mergedPerson.crn
      val personTwoCrn = randomCrn()
      val baseProbationCase = createRandomProbationCase(personTwoCrn)
      val probationPerson = Person.from(baseProbationCase)
      createPersonWithNewKey(probationPerson)
      val deliusAddressIdOne = randomDeliusAddressId()
      val deliusAddressIdTwo = randomDeliusAddressId()

      val responseMergedPerson = ApiResponseSetup.from(mergedPersonDetails).copy(
        addresses = listOf(
          ApiResponseSetupAddress(
            postcode = randomPostcode(),
            deliusAddressId = deliusAddressIdOne,
          ),
          ApiResponseSetupAddress(
            postcode = randomPostcode(),
            deliusAddressId = deliusAddressIdTwo,
          ),
          ApiResponseSetupAddress(
            postcode = randomPostcode(),
            deliusAddressId = randomDeliusAddressId(),
          ),
        ),
      )
      val responsePersonTwo = ApiResponseSetup.from(baseProbationCase, personTwoCrn)

      val responseBody = allProbationCasesResponse(listOf(responseMergedPerson, responsePersonTwo), 1)
      stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)
      stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)

      webTestClient.post()
        .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(PopulateConfig(0))
        .exchange()
        .expectStatus()
        .isOk

      awaitNotNull { personRepository.findByCrn(personTwoCrn)!! }
      awaitAssert {
        val updatedMergedPerson = personRepository.findByCrn(mergedCrn!!)!!
        assertThat(updatedMergedPerson.addresses.size).isEqualTo(0)
      }
    }
  }

  @Nested
  inner class ErrorRecovery {

    @Test
    fun `should retry if request to probation-client fails`() {
      val crn = randomCrn()
      val baseProbationCase = createRandomProbationCase(crn)
      val probationPerson = Person.from(baseProbationCase)
      createPersonWithNewKey(probationPerson)
      val deliusAddressIdOne = randomDeliusAddressId()
      val deliusAddressIdTwo = randomDeliusAddressId()

      val response = ApiResponseSetup.from(baseProbationCase).copy(
        addresses = listOf(
          ApiResponseSetupAddress(
            postcode = randomPostcode(),
            deliusAddressId = deliusAddressIdOne,
          ),
          ApiResponseSetupAddress(
            postcode = randomPostcode(),
            deliusAddressId = deliusAddressIdTwo,
          ),
        ),
      )

      val responseBody = allProbationCasesResponse(listOf(response), 1)
      stubGetRequest(
        url = "/all-probation-cases?page=0&size=500&sort=id,asc",
        scenarioName = "Retry when failed",
        nextScenarioState = "Request will fail",
        body = responseBody,
      )

      stubGetRequest(
        url = "/all-probation-cases?page=0&size=500&sort=id,asc",
        scenarioName = "Retry when failed",
        currentScenarioState = "Request will fail",
        nextScenarioState = "Request will Pass",
        body = responseBody,
        status = 500,
      )

      stubGetRequest(
        url = "/all-probation-cases?page=0&size=500&sort=id,asc",
        scenarioName = "Retry when failed",
        currentScenarioState = "Request will Pass",
        body = responseBody,
      )
      stubPersonMatchScores()
      stubPersonMatchUpsert()

      webTestClient.post()
        .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(PopulateConfig(0))
        .exchange()
        .expectStatus()
        .isOk

      awaitNotNull { addressRepository.findByDeliusAddressId(deliusAddressIdTwo) }
      awaitAssert {
        val updatedPerson = personRepository.findByCrn(probationPerson.crn!!)
        assertThat(updatedPerson?.addresses[0]?.deliusAddressId).isEqualTo(deliusAddressIdOne)
        assertThat(updatedPerson?.addresses[1]?.deliusAddressId).isEqualTo(deliusAddressIdTwo)
      }
    }

    @Nested
    inner class SuccessfulProcessing {

      @Test
      fun `should update and create addresses from single probation case`() {
        val crn = randomCrn()
        val deliusAddressIdOne = randomDeliusAddressId()
        val deliusAddressIdTwo = randomDeliusAddressId()
        val baseProbationCase = createRandomProbationCase(crn)
        val probationPerson = Person.from(baseProbationCase)
        val personEntity = createPersonWithNewKey(probationPerson, configure = addAddressToRecord(Address.from(createRandomProbationAddress()).copy(deliusAddressId = deliusAddressIdOne)))

        val firstDeliusAddress = ApiResponseSetupAddress(
          noFixedAbode = randomBoolean(),
          startDateTime = randomZonedDateTime(),
          endDateTime = randomZonedDateTime(),
          fullAddress = randomFullAddress(),
          buildingName = randomLowerCaseString(),
          addressNumber = randomBuildingNumber(),
          streetName = randomLowerCaseString(),
          district = randomLowerCaseString(),
          townCity = randomLowerCaseString(),
          county = randomLowerCaseString(),
          uprn = randomUprn(),
          notes = randomLowerCaseString(),
          telephoneNumber = randomPhoneNumber(),
          isVerified = randomBoolean(),
          status = ApiResponseSetupAddressStatus(
            code = randomAddressStatusCode().name,
            description = randomAddressStatusCode().description,
          ),
          usage = ApiResponseSetupAddressUsage(
            code = randomAddressUsageCode().name,
            description = randomAddressUsageCode().description,
          ),
          postcode = personEntity.addresses[0].postcode,
          deliusAddressId = deliusAddressIdOne,
        )
        val secondDeliusAddress = firstDeliusAddress.copy(deliusAddressId = deliusAddressIdTwo, postcode = randomPostcode())
        val response = ApiResponseSetup.from(baseProbationCase).copy(
          addresses = listOf(
            firstDeliusAddress,
            secondDeliusAddress,
          ),
        )

        val responseBody = allProbationCasesResponse(listOf(response), 1)
        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)
        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)
        stubPersonMatchScores()
        stubPersonMatchUpsert()

        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .contentType(APPLICATION_JSON)
          .bodyValue(PopulateConfig(0))
          .exchange()
          .expectStatus()
          .isOk

        awaitNotNull { addressRepository.findByDeliusAddressId(deliusAddressIdTwo) }
        awaitAssert {
          val updatedPerson = personRepository.findByCrn(probationPerson.crn!!)!!
          val firstAddress = updatedPerson.addresses.first { it.deliusAddressId == deliusAddressIdOne }
          val secondAddress = updatedPerson.addresses.first { it.deliusAddressId == deliusAddressIdTwo }

          assertThat(firstAddress.deliusAddressId).isEqualTo(deliusAddressIdOne)
          assertThat(firstAddress.postcode).isEqualTo(firstDeliusAddress.postcode)
          assertThat(firstAddress.fullAddress).isEqualTo(firstDeliusAddress.fullAddress)
          assertThat(firstAddress.startDate).isEqualTo(firstDeliusAddress.startDateTime)
          assertThat(firstAddress.endDate).isEqualTo(firstDeliusAddress.endDateTime)
          assertThat(firstAddress.uprn).isEqualTo(firstDeliusAddress.uprn)
          assertThat(firstAddress.buildingName).isEqualTo(firstDeliusAddress.buildingName)
          assertThat(firstAddress.buildingNumber).isEqualTo(firstDeliusAddress.addressNumber)
          assertThat(firstAddress.comment).isEqualTo(firstDeliusAddress.notes)
          assertThat(firstAddress.noFixedAbode).isEqualTo(firstDeliusAddress.noFixedAbode)

          assertThat(firstAddress.thoroughfareName).isEqualTo(firstDeliusAddress.streetName)
          assertThat(firstAddress.dependentLocality).isEqualTo(firstDeliusAddress.district)
          assertThat(firstAddress.postTown).isEqualTo(firstDeliusAddress.townCity)
          assertThat(firstAddress.county).isEqualTo(firstDeliusAddress.county)
          assertThat(firstAddress.isVerified).isEqualTo(firstDeliusAddress.isVerified)
          assertThat(firstAddress.contacts.first().contactValue).isEqualTo(firstDeliusAddress.telephoneNumber)
          assertThat(firstAddress.statusCode).isEqualTo(AddressStatusCode.fromProbation(firstDeliusAddress.status!!.code!!))
          assertThat(firstAddress.usages.first().usageCode).isEqualTo(AddressUsageCode.from(firstDeliusAddress.usage?.code!!))
          assertThat(firstAddress.usages.first().active).isEqualTo(true)

          assertThat(secondAddress.deliusAddressId).isEqualTo(deliusAddressIdTwo)
          assertThat(secondAddress.postcode).isEqualTo(secondDeliusAddress.postcode)
        }
      }

      @Test
      fun `should maintain correct id fields when updating an address`() {
        val crn = randomCrn()
        val deliusAddressId = randomDeliusAddressId()

        val baseProbationCase = createRandomProbationCase(crn)
        val probationPerson = Person.from(baseProbationCase)
        val personEntity = createPersonWithNewKey(
          probationPerson,
          configure = addAddressToRecord(Address.from(createRandomProbationAddress()).copy(deliusAddressId = deliusAddressId)),
        )
        val addressUpdateId = personEntity.addresses.first().updateId!!

        val deliusAddressResponse = ApiResponseSetupAddress(
          noFixedAbode = randomBoolean(),
          startDateTime = randomZonedDateTime(),
          endDateTime = randomZonedDateTime(),
          fullAddress = randomFullAddress(),
          buildingName = randomLowerCaseString(),
          addressNumber = randomBuildingNumber(),
          streetName = randomLowerCaseString(),
          district = randomLowerCaseString(),
          townCity = randomLowerCaseString(),
          county = randomLowerCaseString(),
          uprn = randomUprn(),
          notes = randomLowerCaseString(),
          telephoneNumber = randomPhoneNumber(),
          isVerified = randomBoolean(),
          status = ApiResponseSetupAddressStatus(
            code = randomAddressStatusCode().name,
            description = randomAddressStatusCode().description,
          ),
          usage = ApiResponseSetupAddressUsage(
            code = randomAddressUsageCode().name,
            description = randomAddressUsageCode().description,
          ),
          postcode = personEntity.addresses[0].postcode,
          deliusAddressId = deliusAddressId,
        )
        val response = ApiResponseSetup.from(baseProbationCase).copy(addresses = listOf(deliusAddressResponse))

        val responseBody = allProbationCasesResponse(listOf(response), 1)
        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)
        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)

        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .contentType(APPLICATION_JSON)
          .bodyValue(PopulateConfig(0))
          .exchange()
          .expectStatus()
          .isOk

        awaitAssert {
          val updatedPerson = personRepository.findByCrn(probationPerson.crn!!)
          val firstAddress = updatedPerson?.addresses?.first()!!

          assertThat(firstAddress.deliusAddressId).isEqualTo(deliusAddressId)
          assertThat(firstAddress.updateId).isEqualTo(addressUpdateId)
        }
      }

      @Test
      fun `should create address when probation send address with null postcode`() {
        val crn = randomCrn()
        val baseProbationCase = createRandomProbationCase(crn)
        val probationPerson = Person.from(baseProbationCase)
        createPersonWithNewKey(probationPerson)
        val deliusAddressId = randomDeliusAddressId()

        val deliusAddressResponse = ApiResponseSetupAddress(
          noFixedAbode = randomBoolean(),
          startDateTime = randomZonedDateTime(),
          endDateTime = randomZonedDateTime(),
          fullAddress = randomFullAddress(),
          buildingName = randomLowerCaseString(),
          addressNumber = randomBuildingNumber(),
          streetName = randomLowerCaseString(),
          district = randomLowerCaseString(),
          townCity = randomLowerCaseString(),
          county = randomLowerCaseString(),
          uprn = randomUprn(),
          notes = randomLowerCaseString(),
          telephoneNumber = randomPhoneNumber(),
          isVerified = randomBoolean(),
          status = ApiResponseSetupAddressStatus(
            code = randomAddressStatusCode().name,
            description = randomAddressStatusCode().description,
          ),
          usage = ApiResponseSetupAddressUsage(
            code = randomAddressUsageCode().name,
            description = randomAddressUsageCode().description,
          ),
          postcode = "",
          deliusAddressId = deliusAddressId,
        )
        val response = ApiResponseSetup.from(baseProbationCase).copy(addresses = listOf(deliusAddressResponse))

        val responseBody = allProbationCasesResponse(listOf(response), 1)
        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)
        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)

        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .contentType(APPLICATION_JSON)
          .bodyValue(PopulateConfig(0))
          .exchange()
          .expectStatus()
          .isOk

        awaitAssert {
          val updatedPerson = personRepository.findByCrn(probationPerson.crn!!)
          val firstAddress = updatedPerson?.addresses!!

          assertThat(firstAddress.size).isEqualTo(1)
        }
      }

      @Test
      fun `should start from page 2`() {
        val personOneCrn = randomCrn()
        val personTwoCrn = randomCrn()
        val personThreeCrn = randomCrn()
        val baseProbationCase = createRandomProbationCase(personOneCrn)
        val probationPersonOne = Person.from(baseProbationCase)
        val probationPersonTwo = Person.from(baseProbationCase).copy(crn = personTwoCrn)
        val probationPersonThree = Person.from(baseProbationCase).copy(crn = personThreeCrn)
        createPersonWithNewKey(probationPersonOne)
        createPersonWithNewKey(probationPersonTwo)
        createPersonWithNewKey(probationPersonThree)
        val deliusAddressIdOne = randomDeliusAddressId()
        val deliusAddressIdTwo = randomDeliusAddressId()
        val deliusAddressIdThree = randomDeliusAddressId()
        val deliusAddressIdFour = randomDeliusAddressId()
        val deliusAddressIdFive = randomDeliusAddressId()
        val deliusAddressIdSix = randomDeliusAddressId()

        val responsePersonOne = ApiResponseSetup.from(baseProbationCase).copy(
          addresses = listOf(
            ApiResponseSetupAddress(
              postcode = randomPostcode(),
              deliusAddressId = deliusAddressIdOne,
            ),
            ApiResponseSetupAddress(
              postcode = randomPostcode(),
              deliusAddressId = deliusAddressIdTwo,
            ),
          ),

        )
        val responsePersonTwo = ApiResponseSetup.from(baseProbationCase, personTwoCrn).copy(
          addresses = listOf(
            ApiResponseSetupAddress(
              postcode = randomPostcode(),
              deliusAddressId = deliusAddressIdThree,
            ),
            ApiResponseSetupAddress(
              postcode = randomPostcode(),
              deliusAddressId = deliusAddressIdFour,
            ),
          ),
        )

        val responsePersonThree = ApiResponseSetup.from(baseProbationCase, personThreeCrn).copy(
          addresses = listOf(
            ApiResponseSetupAddress(
              postcode = randomPostcode(),
              deliusAddressId = deliusAddressIdFive,
            ),
            ApiResponseSetupAddress(
              postcode = randomPostcode(),
              deliusAddressId = deliusAddressIdSix,
            ),
          ),
        )

        val firstPageResponseBody = allProbationCasesResponse(listOf(responsePersonOne, responsePersonTwo), 2)
        val secondPageResponseBody = allProbationCasesResponse(listOf(responsePersonThree), 2)

        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = firstPageResponseBody)

        stubGetRequest(url = "/all-probation-cases?page=1&size=500&sort=id,asc", body = secondPageResponseBody)
        stubPersonMatchScores()
        stubPersonMatchUpsert()

        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .contentType(APPLICATION_JSON)
          .bodyValue(PopulateConfig(1))
          .exchange()
          .expectStatus()
          .isOk

        awaitNotNull { addressRepository.findByDeliusAddressId(deliusAddressIdSix) }
        awaitAssert {
          val updatedPersonOne = personRepository.findByCrn(probationPersonOne.crn!!)
          val updatedPersonTwo = personRepository.findByCrn(probationPersonTwo.crn!!)
          val updatedPersonThree = personRepository.findByCrn(probationPersonThree.crn!!)
          // first page is not processed as we are starting from page 2
          assertThat(updatedPersonOne?.addresses).isEmpty()
          assertThat(updatedPersonTwo?.addresses).isEmpty()
          val five = updatedPersonThree!!.addresses.first { it.deliusAddressId == deliusAddressIdFive }
          val six = updatedPersonThree.addresses.first { it.deliusAddressId == deliusAddressIdSix }
          assertThat(five.deliusAddressId).isEqualTo(deliusAddressIdFive)
          assertThat(six.deliusAddressId).isEqualTo(deliusAddressIdSix)
        }
      }
    }
  }

  companion object {
    private const val ADMIN_POPULATE_FROM_PROBATION_URL = "/admin/populate-from-probation"
  }

  fun allProbationCasesResponse(probationCases: List<ApiResponseSetup>, totalPages: Int = 4) = """
  {
    "content": [
        ${probationCases.joinToString { probationCaseResponse(it) }}
    ],
    "page": {
        "size": 2,
        "number": 10,
        "totalElements": 102,
        "totalPages": $totalPages
    }
 }
  """.trimIndent()
}
