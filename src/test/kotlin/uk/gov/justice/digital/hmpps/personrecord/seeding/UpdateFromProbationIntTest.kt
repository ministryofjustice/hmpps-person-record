package uk.gov.justice.digital.hmpps.personrecord.seeding

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.SentenceInfo
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.seeding.responses.allProbationCasesResponse
import uk.gov.justice.digital.hmpps.personrecord.seeding.responses.allProbationCasesSingleResponse
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import java.time.LocalDate

class UpdateFromProbationIntTest : WebTestBase() {

  @Test
  fun `update from probation`() {
    val scenarioName = "update"
    val crnOne: String = randomCrn()
    val crnTwo: String = randomCrn()
    val crnThree: String = randomCrn()
    val crnFour: String = randomCrn()
    val crnFive: String = randomCrn()
    val crnSix: String = randomCrn()
    val crnSeven: String = randomCrn()
    createPerson(
      Person(
        firstName = randomName(),
        lastName = randomName(),
        dateOfBirth = LocalDate.of(1975, 1, 1),
        sentences = listOf(SentenceInfo(LocalDate.of(1991, 7, 22))),
        sourceSystem = DELIUS,
        crn = crnSeven,
      ),
    )
    stubResponse(crnOne, "POPOne", crnTwo, "POPTwo", 0, scenarioName, STARTED)
    stubResponse(crnThree, "POPThree", crnFour, "POPFour", 1, scenarioName, STARTED)
    stubResponse(crnFive, "POPFive", crnSix, "POPSix", 2, scenarioName, STARTED)
    stubSingleResponse(crnSeven, "POPSeven", 3, scenarioName)

    webTestClient.post()
      .uri("/updatefromprobation")
      .exchange()
      .expectStatus()
      .isOk

    awaitNotNullPerson { personRepository.findByCrn(crnSix) }
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
    val newSentenceDate = LocalDate.of(2021, 11, 4)
    assertThat(popSeven.sentenceInfo[0].sentenceDate).isEqualTo(newSentenceDate)
  }

  @Test
  fun `start on page 2`() {
    val scenarioName = "update"
    val crnOne: String = randomCrn()
    val crnTwo: String = randomCrn()
    val crnThree: String = randomCrn()
    val crnFour: String = randomCrn()
    val crnFive: String = randomCrn()
    val crnSix: String = randomCrn()
    val crnSeven: String = randomCrn()

    stubResponse(crnFive, "POPFive", crnSix, "POPSix", 2, scenarioName, STARTED)
    stubSingleResponse(crnSeven, "POPSeven", 3, scenarioName)

    webTestClient.post()
      .uri("/updatefromprobation?startPage=2")
      .exchange()
      .expectStatus()
      .isOk

    awaitNotNullPerson { personRepository.findByCrn(crnSeven) }
    assertThat(personRepository.findByCrn(crnOne)).isNull()
    assertThat(personRepository.findByCrn(crnTwo)).isNull()
    assertThat(personRepository.findByCrn(crnThree)).isNull()
    assertThat(personRepository.findByCrn(crnFour)).isNull()
    assertThat(personRepository.findByCrn(crnFive)!!.firstName).isEqualTo("POPFiveFirstName")
    assertThat(personRepository.findByCrn(crnSix)!!.firstName).isEqualTo("POPSixFirstName")
    val popSeven = personRepository.findByCrn(crnSeven)!!
    assertThat(popSeven.firstName).isEqualTo("POPSevenFirstName")
    assertThat(popSeven.middleNames).isEqualTo("")
    assertThat(popSeven.references.getType(IdentifierType.CRO)).isEqualTo(emptyList<ReferenceEntity>())
    assertThat(popSeven.pseudonyms.size).isEqualTo(0)
    val newSentenceDate = LocalDate.of(2021, 11, 4)
    assertThat(popSeven.sentenceInfo[0].sentenceDate).isEqualTo(newSentenceDate)
  }

  private fun stubResponse(firstCrn: String, firstPrefix: String, secondCrn: String, secondPrefix: String, page: Int, scenarioName: String, scenarioState: String, totalPages: Int = 4) = stubGetRequest(
    url = "/all-probation-cases?size=2&page=$page&sort=id%2Casc",
    scenarioName = scenarioName,
    currentScenarioState = scenarioState,
    body = allProbationCasesResponse(firstCrn, firstPrefix, secondCrn, secondPrefix, totalPages),
  )

  private fun stubSingleResponse(firstCrn: String, firstPrefix: String, page: Int, scenarioName: String) = stubGetRequest(
    url = "/all-probation-cases?size=2&page=$page&sort=id%2Casc",
    scenarioName = scenarioName,
    currentScenarioState = STARTED,
    body = allProbationCasesSingleResponse(firstCrn, firstPrefix),
  )
}
