package uk.gov.justice.digital.hmpps.personrecord.seeding

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.integration.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.name.Names
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.test.responses.allProbationCasesResponse
import uk.gov.justice.digital.hmpps.personrecord.test.responses.allProbationCasesSingleResponse
import java.time.LocalDate
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit.SECONDS

class PopulateFromProbationIntTest : WebTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepository

  @Test
  fun `populate from probation`() {
    val scenarioName = "populate"
    val crnOne: String = randomUUID().toString()
    val crnTwo: String = randomUUID().toString()
    val crnThree: String = randomUUID().toString()
    val crnFour: String = randomUUID().toString()
    val crnFive: String = randomUUID().toString()
    val crnSix: String = randomUUID().toString()
    val crnSeven: String = randomUUID().toString()
    stubResponse(crnOne, "POPOne", crnTwo, "POPTwo", 0, scenarioName, STARTED)
    stubResponse(crnThree, "POPThree", crnFour, "POPFour", 1, scenarioName, STARTED)
    stubResponse(crnFive, "POPFive", crnSix, "POPSix", 2, scenarioName, STARTED)
    stubSingleResponse(crnSeven, "POPSeven", 3, scenarioName, STARTED)

    webTestClient.post()
      .uri("/populatefromprobation")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByCrn(crnSeven)
    }

    val popOne = personRepository.findByCrn(crnOne)!!
    assertThat(popOne.getNames().preferred.firstName).isEqualTo("POPOneFirstName")
    assertThat(popOne.getNames().preferred.middleNames).isEqualTo("POPOneMiddleNameOne POPOneMiddleNameTwo")
    assertThat(popOne.getNames().preferred.lastName).isEqualTo("POPOneLastName")
    assertThat(popOne.crn).isEqualTo(crnOne)
    assertThat(popOne.getNames().preferred.dateOfBirth).isEqualTo(LocalDate.of(1980, 8, 29))
    assertThat(popOne.getNames().aliases[0].firstName).isEqualTo("POPOneAliasOneFirstName")
    assertThat(popOne.getNames().aliases[0].middleNames).isEqualTo("POPOneAliasOneMiddleNameOne POPOneAliasOneMiddleNameTwo")
    assertThat(popOne.getNames().aliases[0].lastName).isEqualTo("POPOneAliasOneLastName")
    assertThat(popOne.getNames().aliases[1].firstName).isEqualTo("POPOneAliasTwoFirstName")
    assertThat(popOne.getNames().aliases[1].middleNames).isEqualTo("POPOneAliasTwoMiddleNameOne POPOneAliasTwoMiddleNameTwo")
    assertThat(popOne.getNames().aliases[1].lastName).isEqualTo("POPOneAliasTwoLastName")
    assertThat(popOne.sourceSystem).isEqualTo(DELIUS)
    assertThat(personRepository.findByCrn(crnTwo)!!.getNames().preferred.firstName).isEqualTo("POPTwoFirstName")
    assertThat(personRepository.findByCrn(crnThree)!!.getNames().preferred.firstName).isEqualTo("POPThreeFirstName")
    assertThat(personRepository.findByCrn(crnFour)!!.getNames().preferred.firstName).isEqualTo("POPFourFirstName")
    assertThat(personRepository.findByCrn(crnFive)!!.getNames().preferred.firstName).isEqualTo("POPFiveFirstName")
    assertThat(personRepository.findByCrn(crnSix)!!.getNames().preferred.firstName).isEqualTo("POPSixFirstName")
    val popSeven = personRepository.findByCrn(crnSeven)!!
    assertThat(popSeven.getNames().preferred.firstName).isEqualTo("POPSevenFirstName")
    assertThat(popSeven.getNames().preferred.middleNames).isEqualTo(null)
    assertThat(popSeven.cro).isEqualTo(CROIdentifier.from(""))
    assertThat(popSeven.getNames().aliases.size).isEqualTo(0)
  }

  @Test
  fun `populate from probation retries`() {
    val scenarioName = "retry"
    val crnOne: String = randomUUID().toString()
    val crnTwo: String = randomUUID().toString()

    // first call fails
    wiremock.stubFor(
      WireMock.get("/all-probation-cases?size=2&page=0&sort=id%2Casc")
        .inScenario(scenarioName)
        .whenScenarioStateIs(STARTED)
        .willSetStateTo("next request will time out")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(503),
        ),
    )

    // third call times out
    wiremock.stubFor(
      WireMock.get("/all-probation-cases?size=2&page=0&sort=id%2Casc")
        .inScenario(scenarioName)
        .whenScenarioStateIs("next request will time out")
        .willSetStateTo("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withFixedDelay(500),
        ),
    )

    stubResponse(crnOne, "POPOne", crnTwo, "POPTwo", 0, scenarioName, STARTED, 1)

    webTestClient.post()
      .uri("/populatefromprobation")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByCrn(crnTwo)
    }

    assertThat(personRepository.findByCrn(crnOne)!!.getNames().preferred.firstName).isEqualTo("POPOneFirstName")
    assertThat(personRepository.findByCrn(crnTwo)!!.getNames().preferred.firstName).isEqualTo("POPTwoFirstName")
  }

  private fun stubResponse(firstCrn: String, firstPrefix: String, secondCrn: String, secondPrefix: String, page: Int, scenarioName: String, scenarioState: String, totalPages: Int = 4) {
    wiremock.stubFor(
      WireMock.get("/all-probation-cases?size=2&page=$page&sort=id%2Casc")
        .inScenario(scenarioName)
        .whenScenarioStateIs(scenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(allProbationCasesResponse(firstCrn, firstPrefix, secondCrn, secondPrefix, totalPages))
            .withStatus(200),
        ),
    )
  }

  private fun stubSingleResponse(firstCrn: String, firstPrefix: String, page: Int, scenarioName: String, scenarioState: String) {
    wiremock.stubFor(
      WireMock.get("/all-probation-cases?size=2&page=$page&sort=id%2Casc")
        .inScenario(scenarioName)
        .whenScenarioStateIs(scenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(allProbationCasesSingleResponse(firstCrn, firstPrefix))
            .withStatus(200),
        ),
    )
  }
}
