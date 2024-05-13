package uk.gov.justice.digital.hmpps.personrecord.seeding

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS

class PopulateFromProbationIntTest : IntegrationTestBase() {

  @Test
  @Transactional // Is this dodgy?
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
    assertThat(pop.firstName).isEqualTo("POPOneFirstName")
    assertThat(pop.middleNames).isEqualTo("POPOneMiddleNameOne POPOneMiddleNameTwo")
    assertThat(pop.lastName).isEqualTo("POPOneLastName")
    assertThat(pop.crn).isEqualTo("D001022")
    assertThat(pop.dateOfBirth).isEqualTo(LocalDate.of(1980, 8, 29))
    assertThat(pop.aliases[0].firstName).isEqualTo("POPOneAliasOneFirstName")
    assertThat(pop.aliases[0].middleNames).isEqualTo("POPOneAliasOneMiddleNameOne POPOneAliasOneMiddleNameTwo")
    assertThat(pop.aliases[0].lastName).isEqualTo("POPOneAliasOneLastName")
    assertThat(pop.aliases[1].firstName).isEqualTo("POPOneAliasTwoFirstName")
    assertThat(pop.aliases[1].middleNames).isEqualTo("POPOneAliasTwoMiddleNameOne POPOneAliasTwoMiddleNameTwo")
    assertThat(pop.aliases[1].lastName).isEqualTo("POPOneAliasTwoLastName")
    assertThat(pop.sourceSystem).isEqualTo(NOMIS)
    assertThat(pops[1].firstName).isEqualTo("POPTwoFirstName")
    assertThat(pops[2].firstName).isEqualTo("POPThreeFirstName")
    assertThat(pops[3].firstName).isEqualTo("POPFourFirstName")
    assertThat(pops[4].firstName).isEqualTo("POPFiveFirstName")
    assertThat(pops[5].firstName).isEqualTo("POPSixFirstName")
    assertThat(pops[6].firstName).isEqualTo("POPSevenFirstName")
    assertThat(pops[6].middleNames).isEqualTo("")
    assertThat(pops[6].cro).isEqualTo(CROIdentifier.from(""))
  }

  @Test
  fun `populate from probation retries`() {
    // first call fails
    wireMockExtension.stubFor(
      WireMock.get("all-probation-cases?page=0&size=2&sort=id,asc")
        .inScenario("retry")
        .whenScenarioStateIs(STARTED)
        .willSetStateTo("next request will fail")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
    // second call fails too
    wireMockExtension.stubFor(
      WireMock.get("all-probation-cases?page=0&size=2&sort=id,asc")
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
    WireMock.get("all-probation-cases?page=0&size=2&sort=id,asc")
      .inScenario("retry")
      .whenScenarioStateIs("next request will time out")
      .willSetStateTo("next request will succeed")
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withFixedDelay(500),
      )

    // Fourth call succeeds
    WireMock.get("all-probation-cases?page=0&size=2&sort=id,asc")
      .inScenario("retry")
      .whenScenarioStateIs("next request will succeed")
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(
            "{\n" +
              "    \"content\": [\n" +
              "        {\n" +
              "            \"identifiers\": {\n" +
              "                \"deliusId\": 2500000501,\n" +
              "                \"crn\": \"D001022\"\n" +
              "            },\n" +
              "            \"name\": {\n" +
              "                \"forename\": \"Test\",\n" +
              "                \"surname\": \"Case\"\n" +
              "            },\n" +
              "            \"dateOfBirth\": \"1980-08-29\",\n" +
              "            \"gender\": {\n" +
              "                \"code\": \"M\",\n" +
              "                \"description\": \"Male\"\n" +
              "            },\n" +
              "            \"aliases\": [],\n" +
              "            \"addresses\": []\n" +
              "        },\n" +
              "        {\n" +
              "            \"identifiers\": {\n" +
              "                \"deliusId\": 2500000503,\n" +
              "                \"crn\": \"D001024\"\n" +
              "            },\n" +
              "            \"name\": {\n" +
              "                \"forename\": \"Glen\",\n" +
              "                \"middleName\": \"ZZZ\",\n" +
              "                \"surname\": \"Abbott\"\n" +
              "            },\n" +
              "            \"dateOfBirth\": \"1990-05-18\",\n" +
              "            \"gender\": {\n" +
              "                \"code\": \"M\",\n" +
              "                \"description\": \"Male\"\n" +
              "            },\n" +
              "            \"aliases\": [],\n" +
              "            \"addresses\": []\n" +
              "        }]}",
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
}
