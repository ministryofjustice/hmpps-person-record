package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
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

@ActiveProfiles("seeding")
class UpdateFromProbationIntTest : WebTestBase() {

  @Test
  fun `update from probation`() {
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
    stubResponse(crnOne, "POPOne", crnTwo, "POPTwo", 0)
    stubResponse(crnThree, "POPThree", crnFour, "POPFour", 1)
    stubResponse(crnFive, "POPFive", crnSix, "POPSix", 2)
    stubSingleResponse(crnSeven, "POPSeven", 3)

    webTestClient.post()
      .uri("/updatefromprobation")
      .exchange()
      .expectStatus()
      .isOk

    awaitNotNullPerson { personRepository.findByCrn(crnSix) }
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
    assertThat(personRepository.findByCrn(crnTwo)!!.getPrimaryName().firstName).isEqualTo("POPTwoFirstName")
    assertThat(personRepository.findByCrn(crnThree)!!.getPrimaryName().firstName).isEqualTo("POPThreeFirstName")
    assertThat(personRepository.findByCrn(crnFour)!!.getPrimaryName().firstName).isEqualTo("POPFourFirstName")
    assertThat(personRepository.findByCrn(crnFive)!!.getPrimaryName().firstName).isEqualTo("POPFiveFirstName")
    assertThat(personRepository.findByCrn(crnSix)!!.getPrimaryName().firstName).isEqualTo("POPSixFirstName")
    val popSeven = personRepository.findByCrn(crnSeven)!!
    assertThat(popSeven.getPrimaryName().firstName).isEqualTo("POPSevenFirstName")
    assertThat(popSeven.getPrimaryName().middleNames).isNull()
    assertThat(popSeven.references.getType(IdentifierType.CRO)).isEqualTo(emptyList<ReferenceEntity>())
    assertThat(popSeven.getAliases().size).isEqualTo(0)
    val newSentenceDate = LocalDate.of(2021, 11, 4)
    assertThat(popSeven.sentenceInfo[0].sentenceDate).isEqualTo(newSentenceDate)
  }

  @Test
  fun `start on page 2`() {
    val crnOne: String = randomCrn()
    val crnTwo: String = randomCrn()
    val crnThree: String = randomCrn()
    val crnFour: String = randomCrn()
    val crnFive: String = randomCrn()
    val crnSix: String = randomCrn()
    val crnSeven: String = randomCrn()

    stubResponse(crnFive, "POPFive", crnSix, "POPSix", 2)
    stubSingleResponse(crnSeven, "POPSeven", 3)

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
    assertThat(personRepository.findByCrn(crnFive)!!.getPrimaryName().firstName).isEqualTo("POPFiveFirstName")
    assertThat(personRepository.findByCrn(crnSix)!!.getPrimaryName().firstName).isEqualTo("POPSixFirstName")
    val popSeven = personRepository.findByCrn(crnSeven)!!
    assertThat(popSeven.getPrimaryName().firstName).isEqualTo("POPSevenFirstName")
    assertThat(popSeven.getPrimaryName().middleNames).isNull()
    assertThat(popSeven.references.getType(IdentifierType.CRO)).isEqualTo(emptyList<ReferenceEntity>())
    assertThat(popSeven.getAliases().size).isEqualTo(0)
    val newSentenceDate = LocalDate.of(2021, 11, 4)
    assertThat(popSeven.sentenceInfo[0].sentenceDate).isEqualTo(newSentenceDate)
  }

  private fun stubResponse(firstCrn: String, firstPrefix: String, secondCrn: String, secondPrefix: String, page: Int) = stubGetRequest(
    url = "/all-probation-cases?size=2&page=$page&sort=id,asc",
    body = allProbationCasesResponse(firstCrn, firstPrefix, secondCrn, secondPrefix, 4),
  )

  private fun stubSingleResponse(firstCrn: String, firstPrefix: String, page: Int) = stubGetRequest(
    url = "/all-probation-cases?size=2&page=$page&sort=id,asc",
    body = allProbationCasesSingleResponse(firstCrn, firstPrefix),
  )
}
