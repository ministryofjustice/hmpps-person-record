package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.seeding.responses.onePrisoner
import uk.gov.justice.digital.hmpps.personrecord.seeding.responses.prisonNumbersResponse
import uk.gov.justice.digital.hmpps.personrecord.seeding.responses.twoPrisoners
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate

@ActiveProfiles("seeding")
class PopulateFromPrisonIntTest : WebTestBase() {

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
    stubGetRequest(
      url = "/api/prisoners/prisoner-numbers?size=2&page=${0}",
      scenarioName = scenarioName,
      body = prisonNumbersResponse(listOf<String?>(prisonNumberOne, prisonNumberTwo)),
    )

    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      requestBody = """{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}""",
      scenarioName = scenarioName,
      responseBody = twoPrisoners(
        prisonNumberOne,
        "PrisonerOne",
        prisonNumberTwo,
        "PrisonerTwo",
      ),
    )
    stubGetRequest(
      url = "/api/prisoners/prisoner-numbers?size=2&page=${1}",
      scenarioName = scenarioName,
      body = prisonNumbersResponse(listOf<String?>(prisonNumberThree, prisonNumberFour)),
    )
    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      requestBody = """{"prisonerNumbers": ["$prisonNumberThree","$prisonNumberFour"]}""",
      scenarioName = scenarioName,
      responseBody = twoPrisoners(
        prisonNumberThree,
        "PrisonerThree",
        prisonNumberFour,
        "PrisonerFour",
      ),
    )
    stubGetRequest(
      url = "/api/prisoners/prisoner-numbers?size=2&page=${2}",
      scenarioName = scenarioName,
      body = prisonNumbersResponse(listOf<String?>(prisonNumberFive, prisonNumberSix)),
    )
    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      requestBody = """{"prisonerNumbers": ["$prisonNumberFive","$prisonNumberSix"]}""",
      scenarioName = scenarioName,
      responseBody = twoPrisoners(
        prisonNumberFive,
        "PrisonerFive",
        prisonNumberSix,
        "PrisonerSix",
      ),
    )

    stubGetRequest(
      url = "/api/prisoners/prisoner-numbers?size=2&page=${3}",
      scenarioName = scenarioName,
      body = prisonNumbersResponse(listOf(prisonNumberSeven, null)),
    )

    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      scenarioName = scenarioName,
      requestBody = """{"prisonerNumbers": ["$prisonNumberSeven"]}""",
      responseBody = onePrisoner(prisonNumberSeven, "PrisonerSeven"),
    )
    webTestClient.post()
      .uri("/populatefromprison")
      .exchange()
      .expectStatus()
      .isOk

    awaitNotNullPerson { personRepository.findByPrisonNumber(prisonNumberSeven) }

    val prisoner1 = personRepository.findByPrisonNumber(prisonNumberOne)!!
    assertThat(prisoner1.firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(prisoner1.middleNames).isEqualTo("PrisonerOneMiddleNameOne PrisonerOneMiddleNameTwo")
    assertThat(prisoner1.lastName).isEqualTo("PrisonerOneLastName")
    assertThat(prisoner1.references.getType(IdentifierType.CRO).first().identifierValue).isEqualTo("029906/12J")
    assertThat(prisoner1.dateOfBirth).isEqualTo(LocalDate.of(1975, 4, 2))
    assertThat(prisoner1.pseudonyms[0].firstName).isEqualTo("PrisonerOneAliasOneFirstName")
    assertThat(prisoner1.pseudonyms[0].middleNames).isEqualTo("PrisonerOneAliasOneMiddleNameOne PrisonerOneAliasOneMiddleNameTwo")
    assertThat(prisoner1.pseudonyms[0].lastName).isEqualTo("PrisonerOneAliasOneLastName")
    assertThat(prisoner1.pseudonyms[1].firstName).isEqualTo("PrisonerOneAliasTwoFirstName")
    assertThat(prisoner1.pseudonyms[1].middleNames).isEqualTo("PrisonerOneAliasTwoMiddleNameOne PrisonerOneAliasTwoMiddleNameTwo")
    assertThat(prisoner1.pseudonyms[1].lastName).isEqualTo("PrisonerOneAliasTwoLastName")
    assertThat(prisoner1.sourceSystem).isEqualTo(NOMIS)

    val prisoner2 = personRepository.findByPrisonNumber(prisonNumberTwo)!!
    assertThat(prisoner2.firstName).isEqualTo("PrisonerTwoFirstName")

    val prisoner3 = personRepository.findByPrisonNumber(prisonNumberThree)!!
    assertThat(prisoner3.firstName).isEqualTo("PrisonerThreeFirstName")

    val prisoner4 = personRepository.findByPrisonNumber(prisonNumberFour)!!
    assertThat(prisoner4.firstName).isEqualTo("PrisonerFourFirstName")

    val prisoner5 = personRepository.findByPrisonNumber(prisonNumberFive)!!
    assertThat(prisoner5.firstName).isEqualTo("PrisonerFiveFirstName")

    val prisoner6 = personRepository.findByPrisonNumber(prisonNumberSix)!!
    assertThat(prisoner6.firstName).isEqualTo("PrisonerSixFirstName")

    val prisoner7 = personRepository.findByPrisonNumber(prisonNumberSeven)!!
    assertThat(prisoner7.firstName).isEqualTo("PrisonerSevenFirstName")
    assertThat(prisoner7.middleNames).isEqualTo("")
    assertThat(prisoner7.references.getType(IdentifierType.CRO)).isEqualTo(emptyList<ReferenceEntity>())
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

    stubGetRequest(
      url = "/api/prisoners/prisoner-numbers?size=2&page=${0}",
      scenarioName = scenarioName,
      body = prisonNumbersResponse(listOf<String?>(prisonNumberOne, prisonNumberTwo)),
    )

    // first call fails
    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      scenarioName = scenarioName,
      nextScenarioState = "next request will time out",
      status = 500,
      requestBody = """{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}""",
      responseBody = "{}",
    )

    // second call times out
    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      requestBody = """{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}""",
      scenarioName = scenarioName,
      currentScenarioState = "next request will time out",
      nextScenarioState = "next request will succeed",
      responseBody = "{}",
      fixedDelay = 210,
    )

    // Third call succeeds
    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      requestBody = """{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}""",
      scenarioName = scenarioName,
      currentScenarioState = "next request will succeed",
      nextScenarioState = "next request will succeed",
      responseBody = twoPrisoners(
        prisonNumberOne,
        "PrisonerOne",
        prisonNumberTwo,
        "PrisonerTwo",
      ),
    )

    stubGetRequest(
      url = "/api/prisoners/prisoner-numbers?size=2&page=${1}",
      scenarioName = scenarioName,
      currentScenarioState = "next request will succeed",
      nextScenarioState = "next request will succeed",
      body = prisonNumbersResponse(listOf<String?>(prisonNumberThree, prisonNumberFour)),
    )

    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      requestBody = """{"prisonerNumbers": ["$prisonNumberThree","$prisonNumberFour"]}""",
      scenarioName = scenarioName,
      currentScenarioState = "next request will succeed",
      nextScenarioState = "next request will succeed",
      responseBody = twoPrisoners(
        prisonNumberThree,
        "PrisonerThree",
        prisonNumberFour,
        "PrisonerFour",
      ),
    )

    stubGetRequest(
      url = "/api/prisoners/prisoner-numbers?size=2&page=${2}",
      scenarioName = scenarioName,
      currentScenarioState = "next request will succeed",
      nextScenarioState = "next request will succeed",
      body = prisonNumbersResponse(listOf<String?>(prisonNumberFive, prisonNumberSix)),
    )

    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      requestBody = """{"prisonerNumbers": ["$prisonNumberFive","$prisonNumberSix"]}""",
      scenarioName = scenarioName,
      currentScenarioState = "next request will succeed",
      nextScenarioState = "next request will succeed",
      responseBody = twoPrisoners(
        prisonNumberFive,
        "PrisonerFive",
        prisonNumberSix,
        "PrisonerSix",
      ),
    )

    stubGetRequest(
      url = "/api/prisoners/prisoner-numbers?size=2&page=${3}",
      scenarioName = scenarioName,
      currentScenarioState = "next request will succeed",
      nextScenarioState = "next request will succeed",
      body = prisonNumbersResponse(
        listOf(
          prisonNumberSeven,
          null,
        ),
      ),
    )

    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      scenarioName = scenarioName,
      currentScenarioState = "next request will succeed",
      requestBody = """{"prisonerNumbers": ["$prisonNumberSeven"]}""",
      responseBody = onePrisoner(prisonNumberSeven, "PrisonerSeven"),
    )

    webTestClient.post()
      .uri("/populatefromprison")
      .exchange()
      .expectStatus()
      .isOk

    awaitNotNullPerson { personRepository.findByPrisonNumber(prisonNumberSeven) }

    assertThat(personRepository.findByPrisonNumber(prisonNumberOne)?.firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberTwo)?.firstName).isEqualTo("PrisonerTwoFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberThree)?.firstName).isEqualTo("PrisonerThreeFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberFour)?.firstName).isEqualTo("PrisonerFourFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberFive)?.firstName).isEqualTo("PrisonerFiveFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberSix)?.firstName).isEqualTo("PrisonerSixFirstName")
    assertThat(personRepository.findByPrisonNumber(prisonNumberSix)?.pseudonyms?.size).isEqualTo(0)
    assertThat(personRepository.findByPrisonNumber(prisonNumberSeven)?.firstName).isEqualTo("PrisonerSevenFirstName")
  }

  @Test
  fun `populate from prison retries getPrisonNumbers`() {
    val prisonNumberOne: String = randomPrisonNumber()
    val prisonNumberTwo: String = randomPrisonNumber()
    val prisonNumberThree: String = randomPrisonNumber()

    stubGetRequest(
      url = "/api/prisoners/prisoner-numbers?size=2&page=0",
      scenarioName = "retry getPrisonNumbers",
      body = prisonNumbersResponse(listOf(prisonNumberOne, prisonNumberTwo), 2),
    )

    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      requestBody = """{"prisonerNumbers": ["$prisonNumberOne","$prisonNumberTwo"]}""",
      scenarioName = "retry getPrisonNumbers",
      responseBody = twoPrisoners(prisonNumberOne, "prisonNumberOne", prisonNumberTwo, "prisonNumberTwo"),
    )

    stub5xxResponse(
      url = "/api/prisoners/prisoner-numbers?size=2&page=1",
      scenarioName = "retry getPrisonNumbers",
      nextScenarioState = "next request will succeed",
      status = 503,
    )

    stubGetRequest(
      url = "/api/prisoners/prisoner-numbers?size=2&page=1",
      scenarioName = "retry getPrisonNumbers",
      currentScenarioState = "next request will succeed",
      nextScenarioState = "next request will succeed",
      body = prisonNumbersResponse(listOf(prisonNumberThree), 2),
    )

    stubPostRequest(
      url = "/prisoner-search/prisoner-numbers",
      requestBody = """{"prisonerNumbers": ["$prisonNumberThree"]}""",
      scenarioName = "retry getPrisonNumbers",
      currentScenarioState = "next request will succeed",
      responseBody = onePrisoner(prisonNumberThree, "PrisonerThree"),
    )

    webTestClient.post()
      .uri("/populatefromprison")
      .exchange()
      .expectStatus()
      .isOk

    val prisoner = awaitNotNullPerson {
      personRepository.findByPrisonNumber(prisonNumberThree)
    }

    assertThat(prisoner.firstName).isEqualTo("PrisonerThreeFirstName")
  }
}
