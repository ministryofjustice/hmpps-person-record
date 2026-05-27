package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
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
      val originalAddress = mergedPerson.addresses.first()
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
            postcode = probationPerson.addresses[0].postcode,
            deliusAddressId = deliusAddressIdOne,
          ),
          ApiResponseSetupAddress(
            postcode = probationPerson.addresses[1].postcode,
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
        assertThat(updatedMergedPerson.addresses.size).isEqualTo(2)
        assertThat(updatedMergedPerson.addresses.first().deliusAddressId).isNull()
        assertThat(updatedMergedPerson.addresses.first().updateId).isEqualTo(originalAddress.updateId)
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
            postcode = probationPerson.addresses[0].postcode,
            deliusAddressId = deliusAddressIdOne,
          ),
          ApiResponseSetupAddress(
            postcode = probationPerson.addresses[1].postcode,
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

      webTestClient.post()
        .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(PopulateConfig(0))
        .exchange()
        .expectStatus()
        .isOk

      awaitAssert {
        val updatedPerson = personRepository.findByCrn(probationPerson.crn!!)
        assertThat(updatedPerson?.addresses[0]?.deliusAddressId).isEqualTo(deliusAddressIdOne)
        assertThat(updatedPerson?.addresses[1]?.deliusAddressId).isEqualTo(deliusAddressIdTwo)
      }
    }

    @Nested
    inner class SuccessfulProcessing {

      @Test
      fun `should populate addresses from probation single case`() {
        val crn = randomCrn()
        val baseProbationCase = createRandomProbationCase(crn)
        val probationPerson = Person.from(baseProbationCase)
        createPersonWithNewKey(probationPerson)
        val deliusAddressIdOne = randomDeliusAddressId()
        val deliusAddressIdTwo = randomDeliusAddressId()

        val firstAddressSetup = ApiResponseSetupAddress(
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
          postcode = probationPerson.addresses[0].postcode,
          deliusAddressId = deliusAddressIdOne,
        )
        val response = ApiResponseSetup.from(baseProbationCase).copy(
          addresses = listOf(
            firstAddressSetup,
            ApiResponseSetupAddress(
              postcode = probationPerson.addresses[1].postcode,
              deliusAddressId = deliusAddressIdTwo,
            ),
          ),
        )

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
          val firstAddress = updatedPerson?.addresses?.first()

          assertThat(firstAddress?.deliusAddressId).isEqualTo(deliusAddressIdOne)
          assertThat(firstAddress?.postcode).isEqualTo(firstAddressSetup.postcode)
          assertThat(firstAddress?.fullAddress).isEqualTo(firstAddressSetup.fullAddress)
          assertThat(firstAddress?.startDate).isEqualTo(firstAddressSetup.startDateTime)
          assertThat(firstAddress?.endDate).isEqualTo(firstAddressSetup.endDateTime)
          assertThat(firstAddress?.uprn).isEqualTo(firstAddressSetup.uprn)
          assertThat(firstAddress?.buildingName).isEqualTo(firstAddressSetup.buildingName)
          assertThat(firstAddress?.buildingNumber).isEqualTo(firstAddressSetup.addressNumber)
          assertThat(firstAddress?.comment).isEqualTo(firstAddressSetup.notes)
          assertThat(firstAddress?.noFixedAbode).isEqualTo(firstAddressSetup.noFixedAbode)

          assertThat(firstAddress?.thoroughfareName).isEqualTo(firstAddressSetup.streetName)
          assertThat(firstAddress?.dependentLocality).isEqualTo(firstAddressSetup.district)
          assertThat(firstAddress?.postTown).isEqualTo(firstAddressSetup.townCity)
          assertThat(firstAddress?.county).isEqualTo(firstAddressSetup.county)
          assertThat(firstAddress?.isVerified).isEqualTo(firstAddressSetup.isVerified)
          assertThat(firstAddress?.contacts?.first()?.contactValue).isEqualTo(firstAddressSetup.telephoneNumber)
          assertThat(firstAddress?.statusCode).isEqualTo(AddressStatusCode.fromProbation(firstAddressSetup.status!!.code!!))
          assertThat(firstAddress?.usages?.first()?.usageCode).isEqualTo(AddressUsageCode.from(firstAddressSetup.usage?.code!!))
          assertThat(firstAddress?.usages?.first()?.active).isEqualTo(true)

          assertThat(updatedPerson?.addresses[1]?.deliusAddressId).isEqualTo(deliusAddressIdTwo)
        }
      }

      @Test
      fun `should create a record which does not exist`() {
        val crn = randomCrn()
        val baseProbationCase = createRandomProbationCase(crn)
        val probationPerson = Person.from(baseProbationCase)

        val deliusAddressIdOne = randomDeliusAddressId()
        val deliusAddressIdTwo = randomDeliusAddressId()

        val firstAddressSetup = ApiResponseSetupAddress(
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
          postcode = probationPerson.addresses[0].postcode,
          deliusAddressId = deliusAddressIdOne,
        )
        val response = ApiResponseSetup.from(baseProbationCase).copy(
          addresses = listOf(
            firstAddressSetup,
            ApiResponseSetupAddress(
              postcode = probationPerson.addresses[1].postcode,
              deliusAddressId = deliusAddressIdTwo,
            ),
          ),
        )

        val responseBody = allProbationCasesResponse(listOf(response), 1)
        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)
        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)
        stubPersonMatchUpsert()
        stubNoMatchesPersonMatch()
        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .contentType(APPLICATION_JSON)
          .bodyValue(PopulateConfig(0))
          .exchange()
          .expectStatus()
          .isOk

        awaitAssert {
          val updatedPerson = personRepository.findByCrn(probationPerson.crn!!)
          val firstAddress = updatedPerson?.addresses?.first()

          assertThat(firstAddress?.deliusAddressId).isEqualTo(deliusAddressIdOne)
          assertThat(firstAddress?.postcode).isEqualTo(firstAddressSetup.postcode)
          assertThat(firstAddress?.fullAddress).isEqualTo(firstAddressSetup.fullAddress)
          assertThat(firstAddress?.startDate).isEqualTo(firstAddressSetup.startDateTime)
          assertThat(firstAddress?.endDate).isEqualTo(firstAddressSetup.endDateTime)
          assertThat(firstAddress?.uprn).isEqualTo(firstAddressSetup.uprn)
          assertThat(firstAddress?.buildingName).isEqualTo(firstAddressSetup.buildingName)
          assertThat(firstAddress?.buildingNumber).isEqualTo(firstAddressSetup.addressNumber)
          assertThat(firstAddress?.comment).isEqualTo(firstAddressSetup.notes)
          assertThat(firstAddress?.noFixedAbode).isEqualTo(firstAddressSetup.noFixedAbode)

          assertThat(firstAddress?.thoroughfareName).isEqualTo(firstAddressSetup.streetName)
          assertThat(firstAddress?.dependentLocality).isEqualTo(firstAddressSetup.district)
          assertThat(firstAddress?.postTown).isEqualTo(firstAddressSetup.townCity)
          assertThat(firstAddress?.county).isEqualTo(firstAddressSetup.county)
          assertThat(firstAddress?.isVerified).isEqualTo(firstAddressSetup.isVerified)
          assertThat(firstAddress?.contacts?.first()?.contactValue).isEqualTo(firstAddressSetup.telephoneNumber)
          assertThat(firstAddress?.statusCode).isEqualTo(AddressStatusCode.fromProbation(firstAddressSetup.status!!.code!!))
          assertThat(firstAddress?.usages?.first()?.usageCode).isEqualTo(AddressUsageCode.from(firstAddressSetup.usage?.code!!))
          assertThat(firstAddress?.usages?.first()?.active).isEqualTo(true)

          assertThat(updatedPerson?.addresses[1]?.deliusAddressId).isEqualTo(deliusAddressIdTwo)
          checkEventLogExist(probationPerson.crn, CPRLogEvents.CPR_RECORD_SEEDED)
        }
      }

      @Test
      fun `should populate addresses from two people on same page`() {
        val personOneCrn = randomCrn()
        val personTwoCrn = randomCrn()
        val baseProbationCase = createRandomProbationCase(personOneCrn)
        val probationPersonOne = Person.from(baseProbationCase)
        val probationPersonTwo = Person.from(baseProbationCase).copy(crn = personTwoCrn)
        createPersonWithNewKey(probationPersonOne)
        createPersonWithNewKey(probationPersonTwo)
        val deliusAddressIdOne = randomDeliusAddressId()
        val deliusAddressIdTwo = randomDeliusAddressId()
        val deliusAddressIdThree = randomDeliusAddressId()
        val deliusAddressIdFour = randomDeliusAddressId()

        val responsePersonOne = ApiResponseSetup.from(baseProbationCase).copy(
          addresses = listOf(
            ApiResponseSetupAddress(
              postcode = probationPersonOne.addresses[0].postcode,
              deliusAddressId = deliusAddressIdOne,
            ),
            ApiResponseSetupAddress(
              postcode = probationPersonOne.addresses[1].postcode,
              deliusAddressId = deliusAddressIdTwo,
            ),
          ),

        )
        val responsePersonTwo = ApiResponseSetup.from(baseProbationCase, personTwoCrn).copy(
          addresses = listOf(
            ApiResponseSetupAddress(
              postcode = probationPersonTwo.addresses[0].postcode,
              deliusAddressId = deliusAddressIdThree,
            ),
            ApiResponseSetupAddress(
              postcode = probationPersonTwo.addresses[1].postcode,
              deliusAddressId = deliusAddressIdFour,
            ),
          ),
        )

        val responseBody = allProbationCasesResponse(listOf(responsePersonOne, responsePersonTwo), 1)
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
          val updatedPersonOne = personRepository.findByCrn(probationPersonOne.crn!!)
          val updatedPersonTwo = personRepository.findByCrn(probationPersonTwo.crn!!)
          assertThat(updatedPersonOne?.addresses[0]?.deliusAddressId).isEqualTo(deliusAddressIdOne)
          assertThat(updatedPersonOne?.addresses[1]?.deliusAddressId).isEqualTo(deliusAddressIdTwo)
          assertThat(updatedPersonTwo?.addresses[0]?.deliusAddressId).isEqualTo(deliusAddressIdThree)
          assertThat(updatedPersonTwo?.addresses[1]?.deliusAddressId).isEqualTo(deliusAddressIdFour)
        }
      }

      @Test
      fun `should populate addresses from 3 people on 2 separate page`() {
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
              postcode = probationPersonOne.addresses[0].postcode,
              deliusAddressId = deliusAddressIdOne,
            ),
            ApiResponseSetupAddress(
              postcode = probationPersonOne.addresses[1].postcode,
              deliusAddressId = deliusAddressIdTwo,
            ),
          ),

        )
        val responsePersonTwo = ApiResponseSetup.from(baseProbationCase, personTwoCrn).copy(
          addresses = listOf(
            ApiResponseSetupAddress(
              postcode = probationPersonTwo.addresses[0].postcode,
              deliusAddressId = deliusAddressIdThree,
            ),
            ApiResponseSetupAddress(
              postcode = probationPersonTwo.addresses[1].postcode,
              deliusAddressId = deliusAddressIdFour,
            ),
          ),
        )

        val responsePersonThree = ApiResponseSetup.from(baseProbationCase, personThreeCrn).copy(
          addresses = listOf(
            ApiResponseSetupAddress(
              postcode = probationPersonThree.addresses[0].postcode,
              deliusAddressId = deliusAddressIdFive,
            ),
            ApiResponseSetupAddress(
              postcode = probationPersonThree.addresses[1].postcode,
              deliusAddressId = deliusAddressIdSix,
            ),
          ),
        )

        val firstPageResponseBody = allProbationCasesResponse(listOf(responsePersonOne, responsePersonTwo), 2)
        val secondPageResponseBody = allProbationCasesResponse(listOf(responsePersonThree), 2)

        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = firstPageResponseBody)
        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = firstPageResponseBody)
        stubGetRequest(url = "/all-probation-cases?page=1&size=500&sort=id,asc", body = secondPageResponseBody)

        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .contentType(APPLICATION_JSON)
          .bodyValue(PopulateConfig(0))
          .exchange()
          .expectStatus()
          .isOk

        awaitAssert {
          val updatedPersonOne = personRepository.findByCrn(probationPersonOne.crn!!)
          val updatedPersonTwo = personRepository.findByCrn(probationPersonTwo.crn!!)
          val updatedPersonThree = personRepository.findByCrn(probationPersonThree.crn!!)
          assertThat(updatedPersonOne?.addresses[0]?.deliusAddressId).isEqualTo(deliusAddressIdOne)
          assertThat(updatedPersonOne?.addresses[1]?.deliusAddressId).isEqualTo(deliusAddressIdTwo)
          assertThat(updatedPersonTwo?.addresses[0]?.deliusAddressId).isEqualTo(deliusAddressIdThree)
          assertThat(updatedPersonTwo?.addresses[1]?.deliusAddressId).isEqualTo(deliusAddressIdFour)
          assertThat(updatedPersonThree?.addresses[0]?.deliusAddressId).isEqualTo(deliusAddressIdFive)
          assertThat(updatedPersonThree?.addresses[1]?.deliusAddressId).isEqualTo(deliusAddressIdSix)
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
              postcode = probationPersonOne.addresses[0].postcode,
              deliusAddressId = deliusAddressIdOne,
            ),
            ApiResponseSetupAddress(
              postcode = probationPersonOne.addresses[1].postcode,
              deliusAddressId = deliusAddressIdTwo,
            ),
          ),

        )
        val responsePersonTwo = ApiResponseSetup.from(baseProbationCase, personTwoCrn).copy(
          addresses = listOf(
            ApiResponseSetupAddress(
              postcode = probationPersonTwo.addresses[0].postcode,
              deliusAddressId = deliusAddressIdThree,
            ),
            ApiResponseSetupAddress(
              postcode = probationPersonTwo.addresses[1].postcode,
              deliusAddressId = deliusAddressIdFour,
            ),
          ),
        )

        val responsePersonThree = ApiResponseSetup.from(baseProbationCase, personThreeCrn).copy(
          addresses = listOf(
            ApiResponseSetupAddress(
              postcode = probationPersonThree.addresses[0].postcode,
              deliusAddressId = deliusAddressIdFive,
            ),
            ApiResponseSetupAddress(
              postcode = probationPersonThree.addresses[1].postcode,
              deliusAddressId = deliusAddressIdSix,
            ),
          ),
        )

        val firstPageResponseBody = allProbationCasesResponse(listOf(responsePersonOne, responsePersonTwo), 2)
        val secondPageResponseBody = allProbationCasesResponse(listOf(responsePersonThree), 2)

        stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = firstPageResponseBody)

        stubGetRequest(url = "/all-probation-cases?page=1&size=500&sort=id,asc", body = secondPageResponseBody)

        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .contentType(APPLICATION_JSON)
          .bodyValue(PopulateConfig(1))
          .exchange()
          .expectStatus()
          .isOk

        awaitAssert {
          val updatedPersonOne = personRepository.findByCrn(probationPersonOne.crn!!)
          val updatedPersonTwo = personRepository.findByCrn(probationPersonTwo.crn!!)
          val updatedPersonThree = personRepository.findByCrn(probationPersonThree.crn!!)
          // first page is not processed as we are starting from page 2
          assertThat(updatedPersonOne?.addresses[0]?.deliusAddressId).isNull()
          assertThat(updatedPersonOne?.addresses[1]?.deliusAddressId).isNull()
          assertThat(updatedPersonTwo?.addresses[0]?.deliusAddressId).isNull()
          assertThat(updatedPersonTwo?.addresses[1]?.deliusAddressId).isNull()
          assertThat(updatedPersonThree?.addresses[0]?.deliusAddressId).isEqualTo(deliusAddressIdFive)
          assertThat(updatedPersonThree?.addresses[1]?.deliusAddressId).isEqualTo(deliusAddressIdSix)
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
