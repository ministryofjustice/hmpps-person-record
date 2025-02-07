package uk.gov.justice.digital.hmpps.personrecord.seeding

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Address
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Sentences
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.seeding.responses.personMatchRequest
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode

class PopulatePersonMatchIntTest : WebTestBase() {

  @BeforeEach
  fun beforeEach() {
    personRepository.deleteAllInBatch()
    stubPersonMigrate()
  }

  @Test
  fun `populate person match`() {
    val firstName = randomName()
    val middleName = randomName()
    val lastName = randomName()
    val dateOfBirth = randomDate()
    val pnc = randomPnc()
    val cro = randomCro()
    val sentenceDate = randomDate()
    val postcode = randomPostcode()
    val aliasFirstName = randomName()
    val aliasLastName = randomName()
    val aliasDateOfBirth = randomDate()
    val person = createPersonWithNewKey(
      Person.from(
        ProbationCase(
          name = Name(firstName = firstName, middleNames = middleName, lastName = lastName),
          dateOfBirth = dateOfBirth,
          identifiers = Identifiers(
            crn = randomCrn(),
            pnc = PNCIdentifier.from(pnc),
            cro = CROIdentifier.from(cro),
          ),
          aliases = listOf(
            ProbationCaseAlias(
              name = Name(firstName = aliasFirstName, lastName = aliasLastName),
              dateOfBirth = aliasDateOfBirth,
            ),
          ),
          addresses = listOf(Address(postcode = postcode)),
          sentences = listOf(Sentences(sentenceDate)),
        ),
      ),
    )

    webTestClient.post()
      .uri("/populatepersonmatch")
      .exchange()
      .expectStatus()
      .isOk

    val personMatchRecord = PersonMatchRecord(
      matchId = person.matchId.toString(),
      firstName = firstName,
      middleNames = middleName,
      lastName = lastName,
      dateOfBirth = dateOfBirth.toString(),
      sourceSystem = SourceSystemType.DELIUS.name,
      firstNameAliases = listOf(aliasFirstName),
      lastNameAliases = listOf(aliasLastName),
      dateOfBirthAliases = listOf(aliasDateOfBirth.toString()),
      cros = listOf(cro),
      pncs = listOf(pnc),
      postcodes = listOf(postcode),
      sentenceDates = listOf(sentenceDate.toString()),
    )
    val expectedResponse = personMatchRequest(personMatchRecord)

    awaitAssert {
      wiremock.verify(
        1,
        postRequestedFor(urlEqualTo("/person/migrate"))
          .withRequestBody(equalToJson(expectedResponse, true, false)),
      )
    }
  }

  @Test
  fun `populate person match as batch`() {
    // Populate person data
    blitz(2000, 10) {
      createPersonWithNewKey(
        Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
      )
    }

    webTestClient.post()
      .uri("/populatepersonmatch")
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert(7) {
      wiremock.verify(2, postRequestedFor(urlEqualTo("/person/migrate")))
    }
  }

  private fun stubPersonMigrate(scenario: String = BASE_SCENARIO, currentScenarioState: String = STARTED, nextScenarioState: String = STARTED) {
    wiremock.stubFor(
      WireMock.post("/person/migrate")
        .inScenario(scenario)
        .whenScenarioStateIs(currentScenarioState)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }
}
