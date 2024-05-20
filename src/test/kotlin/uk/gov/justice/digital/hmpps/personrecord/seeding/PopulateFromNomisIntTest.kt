package uk.gov.justice.digital.hmpps.personrecord.seeding

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
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
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

class PopulateFromNomisIntTest : WebTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepository

  @Test
  fun `populate from nomis`() {
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

  @Test
  fun `populate from nomis retries get prisoners`() {
    val prisonNumberOne: String = UUID.randomUUID().toString()
    val prisonNumberTwo: String = UUID.randomUUID().toString()
    val prisonNumberThree: String = UUID.randomUUID().toString()
    val prisonNumberFour: String = UUID.randomUUID().toString()
    val prisonNumberFive: String = UUID.randomUUID().toString()
    val prisonNumberSix: String = UUID.randomUUID().toString()
    val prisonNumberSeven: String = UUID.randomUUID().toString()

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
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}"""))
        .inScenario("retry get prisoners")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(twoPrisoners(prisonNumberOne, "PrisonerOne", prisonNumberTwo, "PrisonerTwo")),

        ),
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

    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberThree","$prisonNumberFour"]}"""))
        .inScenario("retry get prisoners")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(twoPrisoners(prisonNumberThree, "PrisonerThree", prisonNumberFour, "PrisonerFour")),

        ),
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
    wiremock.stubFor(
      WireMock.post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson("""{"prisonerNumbers": ["$prisonNumberFive","$prisonNumberSix"]}"""))
        .inScenario("retry get prisoners")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(twoPrisoners(prisonNumberFive, "PrisonerFive", prisonNumberSix, "PrisonerSix")),

        ),
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
    // first call fails
    wiremock.stubFor(
      WireMock.get("/api/prisoners/prisoner-numbers?size=1&page=1")
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
      WireMock.get("/api/prisoners/prisoner-numbers?size=1&page=1")
        .inScenario("retry getPrisonerNumbers")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\n  \"totalPages\": 7,\n  \"totalElements\": 7,\n  \"first\": true,\n  \"last\": false,\n  \"size\": 0,\n  \"content\": [\n    \"prisonerNumberTwo\"\n  ],\n  \"number\": 0,\n  \"sort\": [\n    {\n      \"direction\": \"string\",\n      \"nullHandling\": \"string\",\n      \"ascending\": true,\n      \"property\": \"string\",\n      \"ignoreCase\": true\n    }\n  ],\n  \"numberOfElements\": 0,\n  \"pageable\": {\n    \"offset\": 0,\n    \"sort\": [\n      {\n        \"direction\": \"string\",\n        \"nullHandling\": \"string\",\n        \"ascending\": true,\n        \"property\": \"string\",\n        \"ignoreCase\": true\n      }\n    ],\n    \"pageSize\": 0,\n    \"pageNumber\": 0,\n    \"unpaged\": true,\n    \"paged\": true\n  },\n  \"empty\": true\n}")
            .withStatus(200),

        ),
    )
    webTestClient.post()
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilAsserted {
      assertThat(personRepository.findAll().size).isEqualTo(7)
    }
    val prisoners = personRepository.findAll()
    prisoners.sortBy { it.prisonNumber }
    assertThat(prisoners[0].firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(prisoners[1].firstName).isEqualTo("PrisonerTwoFirstName")
    assertThat(prisoners[2].firstName).isEqualTo("PrisonerThreeFirstName")
    assertThat(prisoners[3].firstName).isEqualTo("PrisonerFourFirstName")
    assertThat(prisoners[4].firstName).isEqualTo("PrisonerFiveFirstName")
    assertThat(prisoners[5].firstName).isEqualTo("PrisonerSixFirstName")
    assertThat(prisoners[6].firstName).isEqualTo("PrisonerSevenFirstName")
  }
}
