package uk.gov.justice.digital.hmpps.personrecord.seeding

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.name.Names
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS

class PopulateFromProbationIntTest : IntegrationTestBase() {

  @Test
  fun `populate from probation`() {
    webTestClient.post()
      .uri("/populatefromprobation")
      .exchange()
      .expectStatus()
      .isOk

    await.atMost(15, SECONDS) untilAsserted {
      assertThat(personRepository.findAll().size).isEqualTo(7)
    }
    val pops = personRepository.findAll()
    val pop = pops[0]
    assertThat(Names.from(pop.names).preferred?.firstName).isEqualTo("POPOneFirstName")
    assertThat(Names.from(pop.names).preferred?.middleNames).isEqualTo("POPOneMiddleNameOne POPOneMiddleNameTwo")
    assertThat(Names.from(pop.names).preferred?.lastName).isEqualTo("POPOneLastName")
    assertThat(pop.crn).isEqualTo("D001022")
    assertThat(Names.from(pop.names).preferred?.dateOfBirth).isEqualTo(LocalDate.of(1980, 8, 29))
    assertThat(Names.from(pop.names).aliases[0].firstName).isEqualTo("POPOneAliasOneFirstName")
    assertThat(Names.from(pop.names).aliases[0].middleNames).isEqualTo("POPOneAliasOneMiddleNameOne POPOneAliasOneMiddleNameTwo")
    assertThat(Names.from(pop.names).aliases[0].lastName).isEqualTo("POPOneAliasOneLastName")
    assertThat(Names.from(pop.names).aliases[1].firstName).isEqualTo("POPOneAliasTwoFirstName")
    assertThat(Names.from(pop.names).aliases[1].middleNames).isEqualTo("POPOneAliasTwoMiddleNameOne POPOneAliasTwoMiddleNameTwo")
    assertThat(Names.from(pop.names).aliases[1].lastName).isEqualTo("POPOneAliasTwoLastName")
    assertThat(pop.sourceSystem).isEqualTo(DELIUS)
    assertThat(Names.from(pops[0].names).preferred?.firstName).isEqualTo("POPOneFirstName")
    assertThat(Names.from(pops[1].names).preferred?.firstName).isEqualTo("POPTwoFirstName")
    assertThat(Names.from(pops[2].names).preferred?.firstName).isEqualTo("POPThreeFirstName")
    assertThat(Names.from(pops[3].names).preferred?.firstName).isEqualTo("POPFourFirstName")
    assertThat(Names.from(pops[4].names).preferred?.firstName).isEqualTo("POPFiveFirstName")
    assertThat(Names.from(pops[5].names).preferred?.firstName).isEqualTo("POPSixFirstName")
    assertThat(Names.from(pops[6].names).preferred?.firstName).isEqualTo("POPSevenFirstName")
    assertThat(Names.from(pops[6].names).preferred?.middleNames).isEqualTo("")
    assertThat(pops[6].cro).isEqualTo(CROIdentifier.from(""))
  }

  @Test
  fun `populate from probation retries`() {
    // first call works
    wireMockExtension.stubFor(
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
    wireMockExtension.stubFor(
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
    wireMockExtension.stubFor(
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
    wireMockExtension.stubFor(
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
    assertThat(Names.from(pops[0].names).preferred?.firstName).isEqualTo("POPOneFirstName")
    assertThat(Names.from(pops[1].names).preferred?.firstName).isEqualTo("POPTwoFirstName")
    assertThat(Names.from(pops[2].names).preferred?.firstName).isEqualTo("POPThreeFirstName")
    assertThat(Names.from(pops[3].names).preferred?.firstName).isEqualTo("POPFourFirstName")
    assertThat(Names.from(pops[4].names).preferred?.firstName).isEqualTo("POPFiveFirstName")
    assertThat(Names.from(pops[5].names).preferred?.firstName).isEqualTo("POPSixFirstName")
    assertThat(Names.from(pops[6].names).preferred?.firstName).isEqualTo("POPSevenFirstName")
    assertThat(Names.from(pops[7].names).aliases.size).isEqualTo(0)
  }
}
