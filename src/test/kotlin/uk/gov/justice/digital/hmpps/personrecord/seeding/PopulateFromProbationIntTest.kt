package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.seeding.responses.allProbationCasesResponse
import uk.gov.justice.digital.hmpps.personrecord.seeding.responses.allProbationCasesSingleResponse
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import java.time.LocalDate

@ActiveProfiles("seeding")
class PopulateFromProbationIntTest : WebTestBase() {

  @Test
  fun `populate from probation`() {
    val scenarioName = "populate"
    val crnOne: String = randomCrn()
    val crnTwo: String = randomCrn()
    val crnThree: String = randomCrn()
    val crnFour: String = randomCrn()
    val crnFive: String = randomCrn()
    val crnSix: String = randomCrn()
    val crnSeven: String = randomCrn()
    stubResponse(crnOne, "POPOne", crnTwo, "POPTwo", 0, scenarioName)
    stubResponse(crnThree, "POPThree", crnFour, "POPFour", 1, scenarioName)
    stubResponse(crnFive, "POPFive", crnSix, "POPSix", 2, scenarioName)
    stubSingleResponse(crnSeven, "POPSeven", 3, scenarioName)

    webTestClient.post()
      .uri("/populatefromprobation")
      .exchange()
      .expectStatus()
      .isOk

    awaitNotNullPerson { personRepository.findByCrn(crnSeven) }
    val popOne = personRepository.findByCrn(crnOne)!!
    assertThat(popOne.firstName).isEqualTo("POPOneFirstName")
    assertThat(popOne.middleNames).isEqualTo("POPOneMiddleNameOne POPOneMiddleNameTwo")
    assertThat(popOne.lastName).isEqualTo("POPOneLastName")
    assertThat(popOne.crn).isEqualTo(crnOne)
    assertThat(popOne.dateOfBirth).isEqualTo(LocalDate.of(1980, 8, 29))
    assertThat(popOne.pseudonyms[0].firstName).isEqualTo("POPOneAliasOneFirstName")
    assertThat(popOne.pseudonyms[0].middleNames).isEqualTo("POPOneAliasOneMiddleNameOne POPOneAliasOneMiddleNameTwo")
    assertThat(popOne.pseudonyms[0].lastName).isEqualTo("POPOneAliasOneLastName")
    assertThat(popOne.pseudonyms[1].firstName).isEqualTo("POPOneAliasTwoFirstName")
    assertThat(popOne.pseudonyms[1].middleNames).isEqualTo("POPOneAliasTwoMiddleNameOne POPOneAliasTwoMiddleNameTwo")
    assertThat(popOne.pseudonyms[1].lastName).isEqualTo("POPOneAliasTwoLastName")
    assertThat(popOne.sourceSystem).isEqualTo(DELIUS)
    assertThat(personRepository.findByCrn(crnTwo)!!.firstName).isEqualTo("POPTwoFirstName")
    assertThat(personRepository.findByCrn(crnThree)!!.firstName).isEqualTo("POPThreeFirstName")
    assertThat(personRepository.findByCrn(crnFour)!!.firstName).isEqualTo("POPFourFirstName")
    assertThat(personRepository.findByCrn(crnFive)!!.firstName).isEqualTo("POPFiveFirstName")
    assertThat(personRepository.findByCrn(crnSix)!!.firstName).isEqualTo("POPSixFirstName")
    val popSeven = personRepository.findByCrn(crnSeven)!!
    assertThat(popSeven.firstName).isEqualTo("POPSevenFirstName")
    assertThat(popSeven.middleNames).isEqualTo("")
    assertThat(popSeven.references.getType(IdentifierType.CRO)).isEqualTo(emptyList<ReferenceEntity>())
    assertThat(popSeven.pseudonyms.size).isEqualTo(0)
  }

  @Test
  fun `populate from probation retries`() {
    val scenarioName = "retry"
    val crnOne: String = randomCrn()
    val crnTwo: String = randomCrn()

    stub5xxResponse("/all-probation-cases?size=2&page=0&sort=id%2Casc", nextScenarioState = "next request will time out", scenarioName = scenarioName, status = 503)

    stubGetRequestWithTimeout("/all-probation-cases?size=2&page=0&sort=id%2Casc", "next request will time out", "next request will succeed")

    stubResponse(crnOne, "POPOne", crnTwo, "POPTwo", 0, scenarioName, 1)

    webTestClient.post()
      .uri("/populatefromprobation")
      .exchange()
      .expectStatus()
      .isOk

    awaitNotNullPerson { personRepository.findByCrn(crnTwo) }

    assertThat(personRepository.findByCrn(crnOne)!!.firstName).isEqualTo("POPOneFirstName")
    assertThat(personRepository.findByCrn(crnTwo)!!.firstName).isEqualTo("POPTwoFirstName")
  }

  private fun stubResponse(
    firstCrn: String,
    firstPrefix: String,
    secondCrn: String,
    secondPrefix: String,
    page: Int,
    scenarioName: String,
    totalPages: Int = 4,
  ) = stubGetRequest(
    url = "/all-probation-cases?size=2&page=$page&sort=id%2Casc",
    scenarioName = scenarioName,
    body = allProbationCasesResponse(firstCrn, firstPrefix, secondCrn, secondPrefix, totalPages),
  )

  private fun stubSingleResponse(firstCrn: String, firstPrefix: String, page: Int, scenarioName: String) = stubGetRequest(
    url = "/all-probation-cases?size=2&page=$page&sort=id%2Casc",
    scenarioName = scenarioName,
    body = allProbationCasesSingleResponse(firstCrn, firstPrefix),
  )
}
