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
import uk.gov.justice.digital.hmpps.personrecord.model.person.name.Names
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.test.responses.onePrisoner
import uk.gov.justice.digital.hmpps.personrecord.test.responses.prisonerNumbersResponse
import uk.gov.justice.digital.hmpps.personrecord.test.responses.twoPrisoners
import java.time.LocalDate
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit.SECONDS

class PopulateFromNomisIntTest : WebTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepository

  @Test
  fun `populate from nomis`() {
    val scenarioName = "populate"
    val prisonNumberOne: String = randomUUID().toString()
    val prisonNumberTwo: String = randomUUID().toString()
    val prisonNumberThree: String = randomUUID().toString()
    val prisonNumberFour: String = randomUUID().toString()
    val prisonNumberFive: String = randomUUID().toString()
    val prisonNumberSix: String = randomUUID().toString()
    val prisonNumberSeven: String = randomUUID().toString()
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
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByPrisonNumber(prisonNumberSeven)
    }

    val prisoner1 = personRepository.findByPrisonNumber(prisonNumberOne)!!
    val prisoner1Names = Names.from(prisoner1.names)
    assertThat(prisoner1Names.preferred.firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(prisoner1Names.preferred.middleNames).isEqualTo("PrisonerOneMiddleNameOne PrisonerOneMiddleNameTwo")
    assertThat(prisoner1Names.preferred.lastName).isEqualTo("PrisonerOneLastName")
    assertThat(prisoner1Names.preferred.type).isEqualTo(NameType.PREFERRED)
    assertThat(prisoner1.pnc).isEqualTo(PNCIdentifier.from("2012/394773H"))
    assertThat(prisoner1.cro).isEqualTo(CROIdentifier.from("29906/12J"))
    assertThat(prisoner1Names.preferred.dateOfBirth).isEqualTo(LocalDate.of(1975, 4, 2))
    assertThat(prisoner1Names.aliases[0].firstName).isEqualTo("PrisonerOneAliasOneFirstName")
    assertThat(prisoner1Names.aliases[0].middleNames).isEqualTo("PrisonerOneAliasOneMiddleNameOne PrisonerOneAliasOneMiddleNameTwo")
    assertThat(prisoner1Names.aliases[0].lastName).isEqualTo("PrisonerOneAliasOneLastName")
    assertThat(prisoner1Names.aliases[0].type).isEqualTo(NameType.ALIAS)
    assertThat(prisoner1Names.aliases[1].firstName).isEqualTo("PrisonerOneAliasTwoFirstName")
    assertThat(prisoner1Names.aliases[1].middleNames).isEqualTo("PrisonerOneAliasTwoMiddleNameOne PrisonerOneAliasTwoMiddleNameTwo")
    assertThat(prisoner1Names.aliases[1].lastName).isEqualTo("PrisonerOneAliasTwoLastName")
    assertThat(prisoner1Names.aliases[1].type).isEqualTo(NameType.ALIAS)
    assertThat(prisoner1.sourceSystem).isEqualTo(NOMIS)

    val prisoner2 = personRepository.findByPrisonNumber(prisonNumberTwo)!!
    assertThat(Names.from(prisoner2.names).preferred.firstName).isEqualTo("PrisonerTwoFirstName")

    val prisoner3 = personRepository.findByPrisonNumber(prisonNumberThree)!!
    assertThat(Names.from(prisoner3.names).preferred.firstName).isEqualTo("PrisonerThreeFirstName")

    val prisoner4 = personRepository.findByPrisonNumber(prisonNumberFour)!!
    assertThat(Names.from(prisoner4.names).preferred.firstName).isEqualTo("PrisonerFourFirstName")

    val prisoner5 = personRepository.findByPrisonNumber(prisonNumberFive)!!
    assertThat(Names.from(prisoner5.names).preferred.firstName).isEqualTo("PrisonerFiveFirstName")

    val prisoner6 = personRepository.findByPrisonNumber(prisonNumberSix)!!
    assertThat(Names.from(prisoner6.names).preferred.firstName).isEqualTo("PrisonerSixFirstName")

    val prisoner7 = personRepository.findByPrisonNumber(prisonNumberSeven)!!
    assertThat(Names.from(prisoner7.names).preferred.firstName).isEqualTo("PrisonerSevenFirstName")
    assertThat(Names.from(prisoner7.names).preferred.middleNames).isEqualTo("")
    assertThat(prisoner7.cro).isEqualTo(CROIdentifier.from(""))
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

    val scenarioName = "retry get prisoners"

    stubNumberPage(prisonNumberOne, prisonNumberTwo, 0, scenarioName, STARTED)

    // first call fails
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
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
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
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
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
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
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByPrisonNumber(prisonNumberSeven)
    }

    assertThat(Names.from(personRepository.findByPrisonNumber(prisonNumberOne)!!.names).preferred.firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(Names.from(personRepository.findByPrisonNumber(prisonNumberTwo)!!.names).preferred.firstName).isEqualTo("PrisonerTwoFirstName")
    assertThat(Names.from(personRepository.findByPrisonNumber(prisonNumberThree)!!.names).preferred.firstName).isEqualTo("PrisonerThreeFirstName")
    assertThat(Names.from(personRepository.findByPrisonNumber(prisonNumberFour)!!.names).preferred.firstName).isEqualTo("PrisonerFourFirstName")
    assertThat(Names.from(personRepository.findByPrisonNumber(prisonNumberFive)!!.names).preferred.firstName).isEqualTo("PrisonerFiveFirstName")
    assertThat(Names.from(personRepository.findByPrisonNumber(prisonNumberSix)!!.names).preferred.firstName).isEqualTo("PrisonerSixFirstName")
    assertThat(Names.from(personRepository.findByPrisonNumber(prisonNumberSix)!!.names).aliases.size).isEqualTo(0)
    assertThat(Names.from(personRepository.findByPrisonNumber(prisonNumberSeven)!!.names).preferred.firstName).isEqualTo("PrisonerSevenFirstName")
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

    assertThat(Names.from(prisoner.names).preferred.firstName).isEqualTo("PrisonerThreeFirstName")
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

  private fun stubSinglePrisonerDetail(prisonNumberSeven: String, scenarioName: String, scenarioState: String) {
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .inScenario(scenarioName)
        .whenScenarioStateIs(scenarioState)
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberSeven"]}"""))
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
            .withBody(prisonerNumbersResponse(listOf(prisonNumberSeven)))
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
            .withBody(prisonerNumbersResponse(listOf(prisonNumberOne, prisonNumberTwo)))
            .withStatus(200),
        ),
    )
  }
}
