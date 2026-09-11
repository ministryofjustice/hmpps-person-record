package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.getType
import uk.gov.justice.digital.hmpps.personrecord.jobs.migration.RetryableProbationUpdater
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationCaseResponse

class LowercasePncCroMigrationJobIntTest(
  @Autowired private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  @Autowired private val retryableProbationUpdater: RetryableProbationUpdater,
) : IntegrationTestBase() {

  val lowercasePncCroMigrationJob = LowercasePncCroMigrationJob(corePersonRecordAndDeliusClient, retryableProbationUpdater)

  @Test
  fun `should populate lowercase PNCs and CROs from Delius`() {
    val personOneCrn = randomCrn()
    val personOneDetails = createRandomProbationCase(personOneCrn)
    createPersonWithNewKey(Person.from(personOneDetails))

    val personTwoCrn = randomCrn()
    val personTwoDetails = createRandomProbationCase(personTwoCrn).withIdentifiers(pnc = null, cro = null)
    val personTwoLowercasePnc = randomLowercasePnc()
    createPersonWithNewKey(Person.from(personTwoDetails))

    val personThreeCrn = randomCrn()
    val personThreeDetails = createRandomProbationCase(personThreeCrn).withIdentifiers(pnc = null, cro = null)
    val personThreeLowercaseCro = randomLowercaseCro()
    createPersonWithNewKey(Person.from(personThreeDetails))

    val personOneResponse = ApiResponseSetup.from(personOneDetails)
    val personTwoResponse = ApiResponseSetup.from(personTwoDetails.withIdentifiers(pnc = personTwoLowercasePnc))
    val personThreeResponse = ApiResponseSetup.from(personThreeDetails.withIdentifiers(cro = personThreeLowercaseCro))

    val responseBody = allProbationCasesResponse(listOf(personOneResponse, personTwoResponse, personThreeResponse), 1)
    stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)
    stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    lowercasePncCroMigrationJob.run()

    awaitAssert {
      checkEventLogExist(personOneCrn, CPRLogEvents.CPR_RECORD_UPDATED, 0)
      checkEventLogExist(personTwoCrn, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(personThreeCrn, CPRLogEvents.CPR_RECORD_UPDATED)

      val personTwo = personRepository.findByCrn(personTwoCrn)!!
      assertThat(personTwo.references.getType(IdentifierType.PNC).first()).isEqualTo(personTwoLowercasePnc.uppercase())

      val personThree = personRepository.findByCrn(personThreeCrn)!!
      assertThat(personThree.references.getType(IdentifierType.CRO).first()).isEqualTo(personThreeLowercaseCro.uppercase())
    }
  }

  @Test
  fun `should not populate lowercase PNCs and CROs from Delius when record is merged`() {
    val person = createPersonWithNewKey(createRandomProbationPersonDetails())
    val mergedPersonDetails = createRandomProbationCase().withIdentifiers(pnc = null, cro = null)
    val mergedPerson = createPerson(Person.from(mergedPersonDetails)) { mergedTo = person.id }
    mergedPerson.assertMergedTo(person)

    val mergedPersonCrn = mergedPerson.crn!!
    val mergedPersonLowercasePnc = randomLowercasePnc()

    val mergedPersonResponse = ApiResponseSetup.from(mergedPersonDetails.withIdentifiers(pnc = mergedPersonLowercasePnc))

    val responseBody = allProbationCasesResponse(listOf(mergedPersonResponse), 1)
    stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)
    stubGetRequest(url = "/all-probation-cases?page=0&size=500&sort=id,asc", body = responseBody)

    lowercasePncCroMigrationJob.run()

    awaitAssert {
      checkEventLogExist(mergedPersonCrn, CPRLogEvents.CPR_RECORD_UPDATED, 0)

      val updatedMergedPerson = personRepository.findByCrn(mergedPersonCrn)!!
      assertThat(updatedMergedPerson.references).isEmpty()
    }
  }

  @Test
  fun `should retry if request to probation-client fails`() {
    val personCrn = randomCrn()
    val personDetails = createRandomProbationCase(personCrn).withIdentifiers(pnc = null, cro = null)
    val personLowercaseCro = randomLowercaseCro()
    createPersonWithNewKey(Person.from(personDetails))

    val personResponse = ApiResponseSetup.from(personDetails.withIdentifiers(cro = personLowercaseCro))

    val responseBody = allProbationCasesResponse(listOf(personResponse), 1)

    stubPersonMatchUpsert()
    stubPersonMatchScores()

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

    lowercasePncCroMigrationJob.run()

    awaitAssert {
      checkEventLogExist(personCrn, CPRLogEvents.CPR_RECORD_UPDATED)
      val updatedPerson = personRepository.findByCrn(personCrn)!!
      assertThat(updatedPerson.references.getType(IdentifierType.CRO).first()).isEqualTo(personLowercaseCro.uppercase())
    }
  }

  fun randomLowercasePnc() = randomLongPnc().lowercase()

  fun randomLowercaseCro() = randomCro().lowercase()

  fun ProbationCase.withIdentifiers(pnc: String? = null, cro: String? = null) = copy(
    identifiers = identifiers.copy(pnc = pnc, cro = cro),
  )

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
