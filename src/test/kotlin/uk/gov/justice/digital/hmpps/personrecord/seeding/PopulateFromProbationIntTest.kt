package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.seeding.responses.allProbationCasesResponse
import uk.gov.justice.digital.hmpps.personrecord.seeding.responses.allProbationCasesSingleResponse
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECORD_SEEDED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import java.time.LocalDate

@ActiveProfiles("seeding")
class PopulateFromProbationIntTest : WebTestBase() {

  @Test
  fun `populate from probation`() {
    val scenarioName = "populate"
    val crnOne: String = randomCrn()
    val crnTwo: String = randomCrn()
    val crnThree: String = randomCrn()
    val crnFour: String = randomCrn()
    val crnFive: String = randomCrn()
    val crnSix: String = randomCrn()
    val crnSeven: String = randomCrn()
    stubResponse(crnOne, "POPOne", crnTwo, "POPTwo", 0, scenarioName)
    stubResponse(crnThree, "POPThree", crnFour, "POPFour", 1, scenarioName)
    stubResponse(crnFive, "POPFive", crnSix, "POPSix", 2, scenarioName)
    stubSingleResponse(crnSeven, "POPSeven", 3, scenarioName)

    webTestClient.post()
      .uri("/populatefromprobation")
      .exchange()
      .expectStatus()
      .isOk

    awaitNotNullPerson { personRepository.findByCrn(crnSeven) }
    val popOne = personRepository.findByCrn(crnOne)!!
    assertThat(popOne.getPrimaryName().firstName).isEqualTo("POPOneFirstName")
    assertThat(popOne.getPrimaryName().middleNames).isEqualTo("POPOneMiddleNameOne POPOneMiddleNameTwo")
    assertThat(popOne.getPrimaryName().lastName).isEqualTo("POPOneLastName")
    assertThat(popOne.crn).isEqualTo(crnOne)
    assertThat(popOne.getPrimaryName().dateOfBirth).isEqualTo(LocalDate.of(1980, 8, 29))
    assertThat(popOne.getAliases()[0].firstName).isEqualTo("POPOneAliasOneFirstName")
    assertThat(popOne.getAliases()[0].middleNames).isEqualTo("POPOneAliasOneMiddleNameOne POPOneAliasOneMiddleNameTwo")
    assertThat(popOne.getAliases()[0].lastName).isEqualTo("POPOneAliasOneLastName")
    assertThat(popOne.getAliases()[1].firstName).isEqualTo("POPOneAliasTwoFirstName")
    assertThat(popOne.getAliases()[1].middleNames).isEqualTo("POPOneAliasTwoMiddleNameOne POPOneAliasTwoMiddleNameTwo")
    assertThat(popOne.getAliases()[1].lastName).isEqualTo("POPOneAliasTwoLastName")
    assertThat(popOne.sourceSystem).isEqualTo(DELIUS)
    val popTwo = personRepository.findByCrn(crnTwo)!!
    assertThat(popTwo.getPrimaryName().firstName).isEqualTo("POPTwoFirstName")
    val popThree = personRepository.findByCrn(crnThree)!!
    assertThat(popThree.getPrimaryName().firstName).isEqualTo("POPThreeFirstName")
    val popFour = personRepository.findByCrn(crnFour)!!
    assertThat(popFour.getPrimaryName().firstName).isEqualTo("POPFourFirstName")
    val popFive = personRepository.findByCrn(crnFive)!!
    assertThat(popFive.getPrimaryName().firstName).isEqualTo("POPFiveFirstName")
    val popSix = personRepository.findByCrn(crnSix)!!
    assertThat(popSix.getPrimaryName().firstName).isEqualTo("POPSixFirstName")
    val popSeven = personRepository.findByCrn(crnSeven)!!
    assertThat(popSeven.getPrimaryName().firstName).isEqualTo("POPSevenFirstName")
    assertThat(popSeven.getPrimaryName().middleNames).isNull()
    assertThat(popSeven.references.getType(IdentifierType.CRO)).isEqualTo(emptyList<ReferenceEntity>())
    assertThat(popSeven.getAliases().size).isEqualTo(0)

    val entries = eventLogRepository.findAll()
    checkEventLog(entries, popOne)
    checkEventLog(entries, popTwo)
    checkEventLog(entries, popThree)
    checkEventLog(entries, popFour)
    checkEventLog(entries, popFive)
    checkEventLog(entries, popSix)
    checkEventLog(entries, popSeven)
  }

  private fun checkEventLog(
    entries: MutableList<EventLogEntity>,
    person: PersonEntity,
  ) {
    val entry = entries.find { it.matchId == person.matchId }!!
    assertThat(entry.personUUID).isEqualTo(person.personKey?.personUUID)
    assertThat(entry.sourceSystem).isEqualTo(DELIUS)
    assertThat(entry.eventType).isEqualTo(CPR_RECORD_SEEDED)
    assertThat(entry.sourceSystemId).isEqualTo(person.crn)
  }

  @Test
  fun `populate from probation retries`() {
    val scenarioName = "retry"
    val crnOne: String = randomCrn()
    val crnTwo: String = randomCrn()

    stub5xxResponse("/all-probation-cases?size=2&page=0&sort=id,asc", nextScenarioState = "next request will time out", scenarioName = scenarioName, status = 503)

    stubGetRequestWithTimeout("/all-probation-cases?size=2&page=0&sort=id,asc", "next request will time out", "next request will succeed")

    stubResponse(crnOne, "POPOne", crnTwo, "POPTwo", 0, scenarioName, 1)

    webTestClient.post()
      .uri("/populatefromprobation")
      .exchange()
      .expectStatus()
      .isOk

    awaitNotNullPerson { personRepository.findByCrn(crnTwo) }

    assertThat(personRepository.findByCrn(crnOne)!!.getPrimaryName().firstName).isEqualTo("POPOneFirstName")
    assertThat(personRepository.findByCrn(crnTwo)!!.getPrimaryName().firstName).isEqualTo("POPTwoFirstName")
  }

  private fun stubResponse(
    firstCrn: String,
    firstPrefix: String,
    secondCrn: String,
    secondPrefix: String,
    page: Int,
    scenarioName: String,
    totalPages: Int = 4,
  ) = stubGetRequest(
    url = "/all-probation-cases?size=2&page=$page&sort=id,asc",
    scenarioName = scenarioName,
    body = allProbationCasesResponse(firstCrn, firstPrefix, secondCrn, secondPrefix, totalPages),
  )

  private fun stubSingleResponse(firstCrn: String, firstPrefix: String, page: Int, scenarioName: String) = stubGetRequest(
    url = "/all-probation-cases?size=2&page=$page&sort=id,asc",
    scenarioName = scenarioName,
    body = allProbationCasesSingleResponse(firstCrn, firstPrefix),
  )
}
