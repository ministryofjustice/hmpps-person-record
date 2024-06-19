package uk.gov.justice.digital.hmpps.personrecord.seeding

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.onePrisoner
import uk.gov.justice.digital.hmpps.personrecord.test.responses.prisonNumbersResponse
import uk.gov.justice.digital.hmpps.personrecord.test.responses.twoPrisoners
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS

@ActiveProfiles("seeding")
class PopulateFromPrisonIntTest : WebTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepository

  @Test
  fun `populate from prison`() {
    val scenarioName = "populate"
    val prisonNumberOne: String = randomPrisonNumber()
    val prisonNumberTwo: String = randomPrisonNumber()
    val prisonNumberThree: String = randomPrisonNumber()
    val prisonNumberFour: String = randomPrisonNumber()
    val prisonNumberFive: String = randomPrisonNumber()
    val prisonNumberSix: String = randomPrisonNumber()
    val prisonNumberSeven: String = randomPrisonNumber()
    stubNumberPage(prisonNumberOne, prisonNumberTwo, 0, scenarioName, STARTED)

    stubPrisonerDetails(
      prisonNumberOne,
      "PrisonerOne",
      prisonNumberTwo,
      "PrisonerTwo",
      scenarioName,
      STARTED,
    )
    stubNumberPage(prisonNumberThree, prisonNumberFour, 1, scenarioName, STARTED)
    stubPrisonerDetails(
      prisonNumberThree,
      "PrisonerThree",
      prisonNumberFour,
      "PrisonerFour",
      scenarioName,
      STARTED,
    )
    stubNumberPage(prisonNumberFive, prisonNumberSix, 2, scenarioName, STARTED)
    stubPrisonerDetails(
      prisonNumberFive,
      "PrisonerFive",
      prisonNumberSix,
      "PrisonerSix",
      scenarioName,
      STARTED,
    )

    stubSingleNumberPage(prisonNumberSeven, scenarioName, STARTED)

    stubSinglePrisonerDetail(prisonNumberSeven, scenarioName, STARTED)
    webTestClient.post()
      .uri("/populatefromprison")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByPrisonNumberAndSourceSystem(prisonNumberSeven)
    }

    val prisoner1 = personRepository.findByPrisonNumberAndSourceSystem(prisonNumberOne)!!
    assertThat(prisoner1.firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(prisoner1.middleNames).isEqualTo("PrisonerOneMiddleNameOne PrisonerOneMiddleNameTwo")
    assertThat(prisoner1.lastName).isEqualTo("PrisonerOneLastName")
    assertThat(prisoner1.pnc).isEqualTo(PNCIdentifier.from("2012/394773H"))
    assertThat(prisoner1.cro).isEqualTo(CROIdentifier.from("29906/12J"))
    assertThat(prisoner1.dateOfBirth).isEqualTo(LocalDate.of(1975, 4, 2))
    assertThat(prisoner1.aliases[0].firstName).isEqualTo("PrisonerOneAliasOneFirstName")
    assertThat(prisoner1.aliases[0].middleNames).isEqualTo("PrisonerOneAliasOneMiddleNameOne PrisonerOneAliasOneMiddleNameTwo")
    assertThat(prisoner1.aliases[0].lastName).isEqualTo("PrisonerOneAliasOneLastName")
    assertThat(prisoner1.aliases[1].firstName).isEqualTo("PrisonerOneAliasTwoFirstName")
    assertThat(prisoner1.aliases[1].middleNames).isEqualTo("PrisonerOneAliasTwoMiddleNameOne PrisonerOneAliasTwoMiddleNameTwo")
    assertThat(prisoner1.aliases[1].lastName).isEqualTo("PrisonerOneAliasTwoLastName")
    assertThat(prisoner1.sourceSystem).isEqualTo(NOMIS)

    val prisoner2 = personRepository.findByPrisonNumberAndSourceSystem(prisonNumberTwo)!!
    assertThat(prisoner2.firstName).isEqualTo("PrisonerTwoFirstName")

    val prisoner3 = personRepository.findByPrisonNumberAndSourceSystem(prisonNumberThree)!!
    assertThat(prisoner3.firstName).isEqualTo("PrisonerThreeFirstName")

    val prisoner4 = personRepository.findByPrisonNumberAndSourceSystem(prisonNumberFour)!!
    assertThat(prisoner4.firstName).isEqualTo("PrisonerFourFirstName")

    val prisoner5 = personRepository.findByPrisonNumberAndSourceSystem(prisonNumberFive)!!
    assertThat(prisoner5.firstName).isEqualTo("PrisonerFiveFirstName")

    val prisoner6 = personRepository.findByPrisonNumberAndSourceSystem(prisonNumberSix)!!
    assertThat(prisoner6.firstName).isEqualTo("PrisonerSixFirstName")

    val prisoner7 = personRepository.findByPrisonNumberAndSourceSystem(prisonNumberSeven)!!
    assertThat(prisoner7.firstName).isEqualTo("PrisonerSevenFirstName")
    assertThat(prisoner7.middleNames).isEqualTo("")
    assertThat(prisoner7.cro).isEqualTo(CROIdentifier.from(""))
  }

  @Test
  fun `populate from prison retries get prisoners`() {
    val prisonNumberOne: String = randomPrisonNumber()
    val prisonNumberTwo: String = randomPrisonNumber()
    val prisonNumberThree: String = randomPrisonNumber()
    val prisonNumberFour: String = randomPrisonNumber()
    val prisonNumberFive: String = randomPrisonNumber()
    val prisonNumberSix: String = randomPrisonNumber()
    val prisonNumberSeven: String = randomPrisonNumber()

    val scenarioName = "retry get prisoners"

    stubNumberPage(prisonNumberOne, prisonNumberTwo, 0, scenarioName, STARTED)

    // first call fails
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
        .inScenario(scenarioName)
        .whenScenarioStateIs(STARTED)
        .willSetStateTo("next request will fail")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )

    // second call fails too
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
        .inScenario(scenarioName)
        .whenScenarioStateIs("next request will fail")
        .willSetStateTo("next request will time out")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(503),
        ),
    )

    // third call times out
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
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

    // Fourth call succeeds
    stubPrisonerDetails(
      prisonNumberOne,
      "PrisonerOne",
      prisonNumberTwo,
      "PrisonerTwo",
      scenarioName,
      "next request will succeed",
    )

    stubNumberPage(prisonNumberThree, prisonNumberFour, 1, scenarioName, "next request will succeed")

    stubPrisonerDetails(
      prisonNumberThree,
      "PrisonerThree",
      prisonNumberFour,
      "PrisonerFour",
      scenarioName,
      "next request will succeed",
    )

    stubNumberPage(prisonNumberFive, prisonNumberSix, 2, scenarioName, "next request will succeed")

    stubPrisonerDetails(
      prisonNumberFive,
      "PrisonerFive",
      prisonNumberSix,
      "PrisonerSix",
      scenarioName,
      "next request will succeed",
    )

    stubSingleNumberPage(prisonNumberSeven, scenarioName, "next request will succeed")

    stubSinglePrisonerDetail(prisonNumberSeven, scenarioName, "next request will succeed")

    webTestClient.post()
      .uri("/populatefromprison")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByPrisonNumberAndSourceSystem(prisonNumberSeven)
    }

    assertThat(personRepository.findByPrisonNumberAndSourceSystem(prisonNumberOne)?.firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(personRepository.findByPrisonNumberAndSourceSystem(prisonNumberTwo)?.firstName).isEqualTo("PrisonerTwoFirstName")
    assertThat(personRepository.findByPrisonNumberAndSourceSystem(prisonNumberThree)?.firstName).isEqualTo("PrisonerThreeFirstName")
    assertThat(personRepository.findByPrisonNumberAndSourceSystem(prisonNumberFour)?.firstName).isEqualTo("PrisonerFourFirstName")
    assertThat(personRepository.findByPrisonNumberAndSourceSystem(prisonNumberFive)?.firstName).isEqualTo("PrisonerFiveFirstName")
    assertThat(personRepository.findByPrisonNumberAndSourceSystem(prisonNumberSix)?.firstName).isEqualTo("PrisonerSixFirstName")
    assertThat(personRepository.findByPrisonNumberAndSourceSystem(prisonNumberSix)?.aliases?.size).isEqualTo(0)
    assertThat(personRepository.findByPrisonNumberAndSourceSystem(prisonNumberSeven)?.firstName).isEqualTo("PrisonerSevenFirstName")
  }

  @Test
  fun `populate from prison retries getPrisonNumbers`() {
    val prisonNumberOne: String = randomPrisonNumber()
    val prisonNumberTwo: String = randomPrisonNumber()
    val prisonNumberThree: String = randomPrisonNumber()

    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=0")
        .inScenario("retry getPrisonNumbers")
        .whenScenarioStateIs(STARTED)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonNumbersResponse(listOf(prisonNumberOne, prisonNumberTwo), 2))
            .withStatus(200),
        ),
    )
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
        .inScenario("retry getPrisonNumbers")
        .whenScenarioStateIs(STARTED)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(twoPrisoners(prisonNumberOne, "prisonNumberOne", prisonNumberTwo, "prisonNumberTwo")),
        ),
    )

    // first call fails
    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=1")
        .inScenario("retry getPrisonNumbers")
        .whenScenarioStateIs(STARTED)
        .willSetStateTo("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(503),
        ),
    )
    // second call succeeds
    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=1")
        .inScenario("retry getPrisonNumbers")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonNumbersResponse(listOf(prisonNumberThree), 2))
            .withStatus(200),

        ),
    )

    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonNumbers": ["$prisonNumberThree"]}"""))
        .inScenario("retry getPrisonNumbers")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(onePrisoner(prisonNumberThree, "PrisonerThree")),
        ),
    )
    webTestClient.post()
      .uri("/populatefromprison")
      .exchange()
      .expectStatus()
      .isOk

    val prisoner = await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByPrisonNumberAndSourceSystem(prisonNumberThree)
    }

    assertThat(prisoner.firstName).isEqualTo("PrisonerThreeFirstName")
  }

  private fun stubPrisonerDetails(
    firstNumber: String,
    firstPrefix: String,
    secondNumber: String,
    secondPrefix: String,
    scenarioName: String,
    scenarioState: String,
  ) {
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonNumbers": ["$firstNumber","$secondNumber"]}"""))
        .inScenario(scenarioName)
        .whenScenarioStateIs(scenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(twoPrisoners(firstNumber, firstPrefix, secondNumber, secondPrefix)),
        ),
    )
  }

  private fun stubSinglePrisonerDetail(prisonNumberSeven: String, scenarioName: String, scenarioState: String) {
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .inScenario(scenarioName)
        .whenScenarioStateIs(scenarioState)
        .withRequestBody(equalToJson("""{"prisonNumbers": ["$prisonNumberSeven"]}"""))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(onePrisoner(prisonNumberSeven, "PrisonerSeven")),
        ),
    )
  }

  private fun stubSingleNumberPage(prisonNumberSeven: String, scenarioName: String, scenarioState: String) {
    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=3")
        .inScenario(scenarioName)
        .whenScenarioStateIs(scenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonNumbersResponse(listOf(prisonNumberSeven)))
            .withStatus(200),
        ),
    )
  }

  private fun stubNumberPage(prisonNumberOne: String, prisonNumberTwo: String, page: Int, scenarioName: String, scenarioState: String) {
    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=$page")
        .inScenario(scenarioName)
        .whenScenarioStateIs(scenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonNumbersResponse(listOf(prisonNumberOne, prisonNumberTwo)))
            .withStatus(200),
        ),
    )
  }
}
