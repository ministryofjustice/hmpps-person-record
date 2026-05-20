package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminReclusterRecord
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDeliusAddressId
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationCaseResponse

class PopulateFromProbationControllerIntTest : WebTestBase() {

  @Nested
  inner class MissingRecord {

    @Test
    fun `should not populate when record is merged`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val mergedPerson = createPerson(createRandomProbationPersonDetails()) { mergedTo = person.id }

      mergedPerson.assertMergedTo(person)

      val request = listOf(AdminReclusterRecord(DELIUS, mergedPerson.crn!!))

      webTestClient.post()
        .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("UUID" to person.personKey?.toString()),
        times = 0,
      )
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

      val request = listOf(AdminReclusterRecord(DELIUS, crn))

      stub5xxResponse(url = "/probation-cases/$crn", scenarioName = "retry")

      val updatedData = ApiResponseSetup.from(baseProbationCase).copy(religion = "Updated Religion")

      stubSingleProbationResponse(
        updatedData,
        scenarioName = "retry",
        currentScenarioState = "Next request will succeed",
      )

      webTestClient.post()
        .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      awaitAssert {
        assertThat(personRepository.findByCrn(crn)?.religion).isEqualTo("Updated Religion")
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

        val responseBody = allProbationCasesResponse(listOf(response))
        stubGetRequest(url = "/all-probation-cases", body = responseBody)

        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .exchange()
          .expectStatus()
          .isOk
        // .isOk

        val updatedPerson = awaitNotNull { personRepository.findByCrn(probationPerson.crn!!) }
        assertThat(updatedPerson.addresses[0].deliusAddressId).isEqualTo(deliusAddressIdOne)
        assertThat(updatedPerson.addresses[1].deliusAddressId).isEqualTo(deliusAddressIdTwo)
      }
    }
  }

  /*
    @Nested
    inner class Merges {

      @Test
      fun `should skip already merged source records and process remaining`() {
        val sourcePerson = createPersonWithNewKey(createRandomProbationPersonDetails())
        val targetPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

        mergeRecord(sourcePerson, targetPerson)

        val request = listOf(
          AdminReclusterRecord(DELIUS, sourcePerson.crn!!),
          AdminReclusterRecord(DELIUS, targetPerson.crn!!),
        )

        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .contentType(APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk

        sourcePerson.assertMergedTo(targetPerson)

        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("CRN" to targetPerson.crn!!),
        )
        checkTelemetry(
          CPR_RECORD_MERGED,
          mapOf(
            "FROM_SOURCE_SYSTEM_ID" to sourcePerson.crn,
            "TO_SOURCE_SYSTEM_ID" to targetPerson.crn,
            "SOURCE_SYSTEM" to "DELIUS",
          ),
          times = 0
        )
      }

      @Test
      fun `should dynamically create target record and merge source into it`() {
        val sourcePerson = createPersonWithNewKey(createRandomProbationPersonDetails())
        val targetCrn = randomCrn()

        val request = listOf(
          AdminReclusterRecord(DELIUS, sourcePerson.crn!!),
          AdminReclusterRecord(DELIUS, targetCrn),
        )

        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .contentType(APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk

        val targetPerson = awaitNotNull { personRepository.findByCrn(targetCrn) }

        sourcePerson.assertMergedTo(targetPerson)

        checkTelemetry(
          CPR_RECORD_CREATED,
          mapOf("CRN" to targetCrn),
        )
        checkTelemetry(
          CPR_RECORD_MERGED,
          mapOf(
            "FROM_SOURCE_SYSTEM_ID" to sourcePerson.crn,
            "TO_SOURCE_SYSTEM_ID" to targetPerson.crn,
            "SOURCE_SYSTEM" to "DELIUS"
          ),
        )
      }

      @Test
      fun `should process remaining records even if source is missing`() {
        val targetCrn = randomCrn()

        val targetPerson = createPersonWithNewKey(createRandomProbationPersonDetails(targetCrn))

        val request = listOf(
          AdminReclusterRecord(DELIUS, "MISSING_SOURCE_CRN"),
          AdminReclusterRecord(DELIUS, targetCrn),
        )

        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .contentType(APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk

        awaitAssert {
          val person = personRepository.findByCrn(targetCrn)
          assertThat(person).isEqualTo(targetPerson)
        }

        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("CRN" to targetCrn),
        )
      }

      @Test
      fun `should merge and process target record`() {
        val sourcePerson = createPersonWithNewKey(createRandomProbationPersonDetails())
        val targetPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

        val request = listOf(AdminReclusterRecord(DELIUS, targetPerson.crn!!))


        probationMergeEventAndResponseSetup(
          eventType = OFFENDER_MERGED,
          sourceCrn = sourcePerson.crn!!,
          targetCrn = targetPerson.crn!!
        )

        webTestClient.post()
          .uri(ADMIN_POPULATE_FROM_PROBATION_URL)
          .contentType(APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isOk

        checkTelemetry(
          CPR_RECORD_MERGED,
          mapOf(
            "FROM_SOURCE_SYSTEM_ID" to sourcePerson.crn,
            "TO_SOURCE_SYSTEM_ID" to targetPerson.crn,
            "SOURCE_SYSTEM" to "DELIUS"
          ),
        )
        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("CRN" to targetPerson.crn),
        )

        sourcePerson.assertMergedTo(targetPerson)
      }


  }

*/
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
