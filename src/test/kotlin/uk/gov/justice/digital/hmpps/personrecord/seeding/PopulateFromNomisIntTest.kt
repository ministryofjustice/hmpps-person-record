package uk.gov.justice.digital.hmpps.personrecord.seeding

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.integration.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.service.helper.onePrisoner
import uk.gov.justice.digital.hmpps.personrecord.service.helper.prisonerNumbersResponse
import uk.gov.justice.digital.hmpps.personrecord.service.helper.twoPrisoners
import java.time.LocalDate
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit.SECONDS

class PopulateFromNomisIntTest : WebTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepository

  @Test
  fun `populate from nomis`() {
    val prisonNumberOne: String = randomUUID().toString()
    val prisonNumberTwo: String = randomUUID().toString()
    val prisonNumberThree: String = randomUUID().toString()
    val prisonNumberFour: String = randomUUID().toString()
    val prisonNumberFive: String = randomUUID().toString()
    val prisonNumberSix: String = randomUUID().toString()
    val prisonNumberSeven: String = randomUUID().toString()
    stubNumberPage(prisonNumberOne, prisonNumberTwo, 0)
    stubPrisonerDetails(
      prisonNumberOne,
      "PrisonerOne",
      prisonNumberTwo,
      "PrisonerTwo",
      "populate",
      STARTED,
    )
    stubNumberPage(prisonNumberThree, prisonNumberFour, 1)
    stubPrisonerDetails(
      prisonNumberThree,
      "PrisonerThree",
      prisonNumberFour,
      "PrisonerFour",
      "populate",
      STARTED,
    )
    stubNumberPage(prisonNumberFive, prisonNumberSix, 2)
    stubPrisonerDetails(
      prisonNumberFive,
      "PrisonerFive",
      prisonNumberSix,
      "PrisonerSix",
      "populate",
      STARTED,
    )

    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=3")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonerNumbersResponse(listOf(prisonNumberSeven)))
            .withStatus(200),
        ),
    )

    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberSeven"]}"""))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(onePrisoner(prisonNumberSeven, "PrisonerSeven")),
        ),
    )
    webTestClient.post()
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByPrisonNumber("1")
    }

    val prisoner1 = personRepository.findByPrisonNumber("1")!!
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

    val prisoner2 = personRepository.findByPrisonNumber("2")!!
    assertThat(prisoner2.firstName).isEqualTo("PrisonerTwoFirstName")

    val prisoner3 = personRepository.findByPrisonNumber("3")!!
    assertThat(prisoner3.firstName).isEqualTo("PrisonerThreeFirstName")

    val prisoner4 = personRepository.findByPrisonNumber("4")!!
    assertThat(prisoner4.firstName).isEqualTo("PrisonerFourFirstName")

    val prisoner5 = personRepository.findByPrisonNumber("5")!!
    assertThat(prisoner5.firstName).isEqualTo("PrisonerFiveFirstName")

    val prisoner6 = personRepository.findByPrisonNumber("6")!!
    assertThat(prisoner6.firstName).isEqualTo("PrisonerSixFirstName")

    val prisoner7 = personRepository.findByPrisonNumber("7")!!
    assertThat(prisoner7.firstName).isEqualTo("PrisonerSevenFirstName")
    assertThat(prisoner7.middleNames).isEqualTo("")
    assertThat(prisoner7.cro).isEqualTo(CROIdentifier.from(""))
  }

  private fun stubNumberPage(prisonNumberOne: String, prisonNumberTwo: String, page: Int) {
    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=$page")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonerNumbersResponse(listOf(prisonNumberOne, prisonNumberTwo)))
            .withStatus(200),
        ),
    )
  }

  @Test
  fun `populate from nomis retries get prisoners`() {
    val prisonNumberOne: String = randomUUID().toString()
    val prisonNumberTwo: String = randomUUID().toString()
    val prisonNumberThree: String = randomUUID().toString()
    val prisonNumberFour: String = randomUUID().toString()
    val prisonNumberFive: String = randomUUID().toString()
    val prisonNumberSix: String = randomUUID().toString()
    val prisonNumberSeven: String = randomUUID().toString()

    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=0")
        .inScenario("retry get prisoners")
        .whenScenarioStateIs(STARTED)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonerNumbersResponse(listOf(prisonNumberOne, prisonNumberTwo)))
            .withStatus(200),
        ),
    )

    // first call fails
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
        .inScenario("retry get prisoners")
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
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
        .inScenario("retry get prisoners")
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
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
        .inScenario("retry get prisoners")
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
      "retry get prisoners",
      "next request will succeed",
    )

    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=1")
        .inScenario("retry get prisoners")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonerNumbersResponse(listOf(prisonNumberThree, prisonNumberFour)))
            .withStatus(200),
        ),
    )

    stubPrisonerDetails(
      prisonNumberThree,
      "PrisonerThree",
      prisonNumberFour,
      "PrisonerFour",
      "retry get prisoners",
      "next request will succeed",
    )

    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=2")
        .inScenario("retry get prisoners")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonerNumbersResponse(listOf(prisonNumberFive, prisonNumberSix)))
            .withStatus(200),
        ),
    )

    stubPrisonerDetails(
      prisonNumberFive,
      "PrisonerFive",
      prisonNumberSix,
      "PrisonerSix",
      "retry get prisoners",
      "next request will succeed",
    )

    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=3")
        .inScenario("retry get prisoners")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonerNumbersResponse(listOf(prisonNumberSeven)))
            .withStatus(200),
        ),
    )

    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberSeven"]}"""))
        .inScenario("retry get prisoners")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(onePrisoner(prisonNumberSeven, "PrisonerSeven")),
        ),
    )

    webTestClient.post()
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    val prisonerOne = await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByPrisonNumber(prisonNumberOne)
    }

    assertThat(prisonerOne.firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberTwo)?.firstName).isEqualTo("PrisonerTwoFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberThree)?.firstName).isEqualTo("PrisonerThreeFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberFour)?.firstName).isEqualTo("PrisonerFourFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberFive)?.firstName).isEqualTo("PrisonerFiveFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberSix)?.firstName).isEqualTo("PrisonerSixFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberSix)?.aliases?.size).isEqualTo(0)
    assertThat(personRepository.findByPrisonNumber(prisonNumberSeven)?.firstName).isEqualTo("PrisonerSevenFirstName")
  }

  @Test
  fun `populate from nomis retries getPrisonerNumbers`() {
    val prisonNumberOne: String = randomUUID().toString()
    val prisonNumberTwo: String = randomUUID().toString()
    val prisonNumberThree: String = randomUUID().toString()

    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=0")
        .inScenario("retry getPrisonerNumbers")
        .whenScenarioStateIs(STARTED)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonerNumbersResponse(listOf(prisonNumberOne, prisonNumberTwo), 2))
            .withStatus(200),
        ),
    )
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
        .inScenario("retry getPrisonerNumbers")
        .whenScenarioStateIs(STARTED)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(twoPrisoners(prisonNumberOne, "prisonerNumberOne", prisonNumberTwo, "prisonerNumberTwo")),
        ),
    )

    // first call fails
    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=2&page=1")
        .inScenario("retry getPrisonerNumbers")
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
        .inScenario("retry getPrisonerNumbers")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(prisonerNumbersResponse(listOf(prisonNumberThree), 2))
            .withStatus(200),

        ),
    )

    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberThree"]}"""))
        .inScenario("retry getPrisonerNumbers")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(onePrisoner(prisonNumberThree, "PrisonerThree")),
        ),
    )
    webTestClient.post()
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    val prisoner = await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByPrisonNumber(prisonNumberThree)
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
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$firstNumber","$secondNumber"]}"""))
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
}
