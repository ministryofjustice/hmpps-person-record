package uk.gov.justice.digital.hmpps.personrecord.seeding

import com.github.tomakehurst.wiremock.client.WireMock
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
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.helper.allProbationCasesResponse
import uk.gov.justice.digital.hmpps.personrecord.service.helper.allProbationCasesSingleResponse
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
    assertThat(popOne.firstName).isEqualTo("POPOneFirstName")
    assertThat(popOne.middleNames).isEqualTo("POPOneMiddleNameOne POPOneMiddleNameTwo")
    assertThat(popOne.lastName).isEqualTo("POPOneLastName")
    assertThat(popOne.crn).isEqualTo(crnOne)
    assertThat(popOne.dateOfBirth).isEqualTo(LocalDate.of(1980, 8, 29))
    assertThat(popOne.aliases[0].firstName).isEqualTo("POPOneAliasOneFirstName")
    assertThat(popOne.aliases[0].middleNames).isEqualTo("POPOneAliasOneMiddleNameOne POPOneAliasOneMiddleNameTwo")
    assertThat(popOne.aliases[0].lastName).isEqualTo("POPOneAliasOneLastName")
    assertThat(popOne.aliases[1].firstName).isEqualTo("POPOneAliasTwoFirstName")
    assertThat(popOne.aliases[1].middleNames).isEqualTo("POPOneAliasTwoMiddleNameOne POPOneAliasTwoMiddleNameTwo")
    assertThat(popOne.aliases[1].lastName).isEqualTo("POPOneAliasTwoLastName")
    assertThat(popOne.sourceSystem).isEqualTo(DELIUS)
    assertThat(personRepository.findByCrn(crnTwo)!!.firstName).isEqualTo("POPTwoFirstName")
    assertThat(personRepository.findByCrn(crnThree)!!.firstName).isEqualTo("POPThreeFirstName")
    assertThat(personRepository.findByCrn(crnFour)!!.firstName).isEqualTo("POPFourFirstName")
    assertThat(personRepository.findByCrn(crnFive)!!.firstName).isEqualTo("POPFiveFirstName")
    assertThat(personRepository.findByCrn(crnSix)!!.firstName).isEqualTo("POPSixFirstName")
    assertThat(personRepository.findByCrn(crnSeven)!!.firstName).isEqualTo("POPSevenFirstName")
    assertThat(personRepository.findByCrn(crnSeven)!!.middleNames).isEqualTo("")
    assertThat(personRepository.findByCrn(crnSeven)!!.cro).isEqualTo(CROIdentifier.from(""))
  }

  @Test
  fun `populate from probation retries`() {
    // first call works
    wiremock.stubFor(
      WireMock.get("/all-probation-cases?size=2&page=0&sort=id%2Casc")
        .inScenario("retry")
        .whenScenarioStateIs(STARTED)
        .willSetStateTo("next request will fail")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              "{\n    \"content\": [\n        {\n            \"identifiers\": {\n                \"deliusId\": 2500000501,\n                \"crn\": \"D001022\"\n            },\n            \"name\": {\n                \"forename\": \"POPOneFirstNameOne\",\n           \"middleName\":\"POPOneMiddleNameOne POPOneMiddleNameTwo\",     \"surname\": \"POPOneLastName\"\n            },\n            \"dateOfBirth\": \"1980-08-29\",\n            \"gender\": {\n                \"code\": \"M\",\n                \"description\": \"Male\"\n            },\n            \"aliases\": [{\"name\": {\n                        \"forename\": \"POPOneAliasOneFirstName\",\n                     \"middleName\":    \"POPOneAliasOneMiddleNameOne POPOneAliasOneMiddleNameTwo\",\n                        \"surname\": \"POPOneAliasOneLastName\"\n                    },\n                    \"dateOfBirth\": \"1967-11-04\"},{\"name\": {\n                        \"forename\": \"POPOneAliasTwoFirstName\",\n            \"middleName\":             \"POPOneAliasTwoMiddleNameOne POPOneAliasTwoMiddleNameTwo\",\n                        \"surname\": \"POPOneAliasTwoLastName\"\n                    },\n                    \"dateOfBirth\": \"1967-11-04\"}],\n            \"addresses\": []\n        },\n        {\n            \"identifiers\": {\n                \"deliusId\": 2500000503,\n                \"crn\": \"D001024\"\n            },\n            \"name\": {\n                \"forename\": \"POPTwoFirstName\",\n           \"middleName\":\"POPTwoMiddleNameOne POPTwoMiddleNameTwo\",     \"surname\": \"POPTwoLastName\"\n            },\n            \"dateOfBirth\": \"1990-05-18\",\n            \"gender\": {\n                \"code\": \"M\",\n                \"description\": \"Male\"\n            },\n            \"aliases\": [],\n            \"addresses\": []\n        }],\n    \"pageable\": {\n        \"pageNumber\": 6004,\n        \"pageSize\": 100,\n        \"sort\": {\n            \"unsorted\": false,\n            \"sorted\": true,\n            \"empty\": false\n        },\n        \"offset\": 600400,\n        \"paged\": true,\n        \"unpaged\": false\n    },\n    \"totalElements\": 7,\n    \"totalPages\": 4,\n    \"last\": false,\n    \"numberOfElements\": 2,\n    \"first\": true,\n    \"size\": 2,\n    \"number\": 1,\n    \"sort\": {\n        \"unsorted\": false,\n        \"sorted\": true,\n        \"empty\": false\n },\n    \"empty\": false}",
            ),
        ),
    )
    // second call fails
    wiremock.stubFor(
      WireMock.get("/all-probation-cases?size=2&page=0&sort=id%2Casc")
        .inScenario("retry")
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
      WireMock.get("/all-probation-cases?size=2&page=0&sort=id%2Casc")
        .inScenario("retry")
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
      WireMock.get("/all-probation-cases?size=2&page=0&sort=id%2Casc")
        .inScenario("retry")
        .whenScenarioStateIs("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              "{\n    \"content\": [\n        {\n            \"identifiers\": {\n                \"deliusId\": 2500000501,\n                \"crn\": \"D001022\"\n            },\n            \"name\": {\n                \"forename\": \"POPOneFirstName\",\n           \"middleName\":\"POPOneMiddleNameOne POPOneMiddleNameTwo\",     \"surname\": \"POPOneLastName\"\n            },\n            \"dateOfBirth\": \"1980-08-29\",\n            \"gender\": {\n                \"code\": \"M\",\n                \"description\": \"Male\"\n            },\n            \"aliases\": [{\"name\": {\n                        \"forename\": \"POPOneAliasOneFirstName\",\n                     \"middleName\":    \"POPOneAliasOneMiddleNameOne POPOneAliasOneMiddleNameTwo\",\n                        \"surname\": \"POPOneAliasOneLastName\"\n                    },\n                    \"dateOfBirth\": \"1967-11-04\"},{\"name\": {\n                        \"forename\": \"POPOneAliasTwoFirstName\",\n            \"middleName\":             \"POPOneAliasTwoMiddleNameOne POPOneAliasTwoMiddleNameTwo\",\n                        \"surname\": \"POPOneAliasTwoLastName\"\n                    },\n                    \"dateOfBirth\": \"1967-11-04\"}],\n            \"addresses\": []\n        },\n        {\n            \"identifiers\": {\n                \"deliusId\": 2500000503,\n                \"crn\": \"D001024\"\n            },\n            \"name\": {\n                \"forename\": \"POPTwoFirstName\",\n           \"middleName\":\"POPTwoMiddleNameOne POPTwoMiddleNameTwo\",     \"surname\": \"POPTwoLastName\"\n            },\n            \"dateOfBirth\": \"1990-05-18\",\n            \"gender\": {\n                \"code\": \"M\",\n                \"description\": \"Male\"\n            },\n            \"aliases\": [],\n            \"addresses\": []\n        }],\n    \"pageable\": {\n        \"pageNumber\": 6004,\n        \"pageSize\": 100,\n        \"sort\": {\n            \"unsorted\": false,\n            \"sorted\": true,\n            \"empty\": false\n        },\n        \"offset\": 600400,\n        \"paged\": true,\n        \"unpaged\": false\n    },\n    \"totalElements\": 7,\n    \"totalPages\": 4,\n    \"last\": false,\n    \"numberOfElements\": 2,\n    \"first\": true,\n    \"size\": 2,\n    \"number\": 1,\n    \"sort\": {\n        \"unsorted\": false,\n        \"sorted\": true,\n        \"empty\": false\n },\n    \"empty\": false}",
            ),
        ),
    )

    webTestClient.post()
      .uri("/populatefromprobation")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilAsserted {
      assertThat(personRepository.findAll().size).isEqualTo(7)
    }

    val pops = personRepository.findAll()
    assertThat(pops[0].firstName).isEqualTo("POPOneFirstName")
    assertThat(pops[1].firstName).isEqualTo("POPTwoFirstName")
    assertThat(pops[2].firstName).isEqualTo("POPThreeFirstName")
    assertThat(pops[3].firstName).isEqualTo("POPFourFirstName")
    assertThat(pops[4].firstName).isEqualTo("POPFiveFirstName")
    assertThat(pops[5].firstName).isEqualTo("POPSixFirstName")
    assertThat(pops[6].firstName).isEqualTo("POPSevenFirstName")
    assertThat(pops[6].aliases.size).isEqualTo(0)
  }

  private fun stubResponse(firstCrn: String, firstPrefix: String, secondCrn: String, secondPrefix: String, page: Int, scenarioName: String, scenarioState: String) {
    wiremock.stubFor(
      WireMock.get("/all-probation-cases?size=2&page=$page&sort=id%2Casc")
        .inScenario(scenarioName)
        .whenScenarioStateIs(scenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(allProbationCasesResponse(firstCrn, firstPrefix, secondCrn, secondPrefix))
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
