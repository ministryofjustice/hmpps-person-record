package uk.gov.justice.digital.hmpps.personrecord.seeding

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.health.PersonMatchHealthPing
import uk.gov.justice.digital.hmpps.personrecord.health.PersonRecordHealthPing
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.responses.allProbationCasesResponse
import uk.gov.justice.digital.hmpps.personrecord.test.responses.allProbationCasesSingleResponse
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS

@ActiveProfiles("seeding")
class PopulateFromProbationIntTest : WebTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepository

  @MockBean
  private lateinit var personMatchHealthPing: PersonMatchHealthPing

  @MockBean
  @Autowired
  private lateinit var personRecordHealthPing: PersonRecordHealthPing

  @Test
  fun `populate from probation`() {
    val scenarioName = "populate"
    val crnOne: String = randomCRN()
    val crnTwo: String = randomCRN()
    val crnThree: String = randomCRN()
    val crnFour: String = randomCRN()
    val crnFive: String = randomCRN()
    val crnSix: String = randomCRN()
    val crnSeven: String = randomCRN()
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
    val crnOne: String = randomCRN()
    val crnTwo: String = randomCRN()

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

    assertThat(personRepository.findByCrn(crnOne)!!.firstName).isEqualTo("POPOneFirstName")
    assertThat(personRepository.findByCrn(crnTwo)!!.firstName).isEqualTo("POPTwoFirstName")
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
